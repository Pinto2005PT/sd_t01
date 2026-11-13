package server;

import common.ErroValidacao;
import common.EstadoAdmin;
import common.EstadoPosto;
import common.Posto;
import common.TipoFicha;
import common.Utilizador;

import common.EntradaHistorico;
import common.EstadoReserva;
import common.Reserva;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Logica do servidor para o cliente geral
public class ServicoGeral {

    // erro do PostgreSQL quando um valor UNIQUE ja existe
    private static final String ERRO_REPETIDO = "23505";

    // regras da reserva
    private static final int MAX_RESERVAS_ATIVAS = 2;
    private static final int MAX_HORAS = 2;

    // formato das datas, ex: 2026-11-20 14:00
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BaseDados bd;

    public ServicoGeral(BaseDados bd) {
        this.bd = bd;
    }

    // registo de utilizador
    public Utilizador registarUtilizador(String nome, String email, String matricula, TipoFicha tipoFicha)
            throws ErroValidacao, SQLException {

        if (nome == null || nome.trim().isEmpty()) {
            throw new ErroValidacao("O nome e obrigatorio.");
        }
        if (email == null || !email.contains("@")) {
            throw new ErroValidacao("Email invalido.");
        }
        if (matricula == null || matricula.trim().isEmpty()) {
            throw new ErroValidacao("A matricula e obrigatoria.");
        }
        if (tipoFicha == null) {
            throw new ErroValidacao("O tipo de ficha e obrigatorio.");
        }

        // RETURNING id devolve o id gerado pela BD
        String sql = "INSERT INTO utilizador (nome, email, matricula, tipo_ficha) "
                   + "VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nome.trim());
            ps.setString(2, email.trim());
            ps.setString(3, matricula.trim());
            ps.setString(4, tipoFicha.name());

            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt("id");

            return new Utilizador(id, nome.trim(), email.trim(), matricula.trim(), tipoFicha);

        } catch (SQLException e) {
            if (ERRO_REPETIDO.equals(e.getSQLState())) {
                throw new ErroValidacao("Ja existe um utilizador com esse email ou matricula.");
            }
            throw e;
        }
    }

    // registo de posto (fica PENDENTE ate o admin aprovar)
    public Posto registarPosto(String codigo, String localizacao, String cidade,
                               double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws ErroValidacao, SQLException {

        validarDadosPosto(codigo, localizacao, cidade, potenciaKw, tipoFicha, precoKwh);

        Posto posto = new Posto(codigo.trim(), localizacao.trim(), cidade.trim(),
                                potenciaKw, tipoFicha, precoKwh);

        String sql = "INSERT INTO posto (codigo, localizacao, cidade, potencia_kw, tipo_ficha, "
                   + "preco_kwh, estado, estado_admin) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        Connection con = null;
        try {
            con = bd.ligar();

            // transacao: posto + historico (ou grava os dois ou nenhum)
            con.setAutoCommit(false);

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, posto.getCodigo());
            ps.setString(2, posto.getLocalizacao());
            ps.setString(3, posto.getCidade());
            ps.setDouble(4, posto.getPotenciaKw());
            ps.setString(5, posto.getTipoFicha().name());
            ps.setDouble(6, posto.getPrecoKwh());
            ps.setString(7, posto.getEstado().name());
            ps.setString(8, posto.getEstadoAdmin().name());

            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt("id");

            Historico.registar(con, id, null, "REGISTO", null, EstadoAdmin.PENDENTE.name(),
                               "Posto registado");

            con.commit();

            return new Posto(id, posto.getCodigo(), posto.getLocalizacao(), posto.getCidade(),
                             posto.getPotenciaKw(), posto.getTipoFicha(), posto.getPrecoKwh(),
                             EstadoPosto.DISPONIVEL, EstadoAdmin.PENDENTE);

        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            if (ERRO_REPETIDO.equals(e.getSQLState())) {
                throw new ErroValidacao("Ja existe um posto com o codigo " + codigo + ".");
            }
            throw e;

        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // listagem dos postos aprovados com filtros (cidade, ficha, potencia minima)
    public List<Posto> listarPostos(String cidade, TipoFicha tipoFicha, double potenciaMinima)
            throws SQLException {

        // filtro vazio -> '%' (no LIKE quer dizer qualquer coisa)
        String filtroCidade = (cidade == null || cidade.trim().isEmpty()) ? "%" : cidade.trim();
        String filtroFicha  = (tipoFicha == null) ? "%" : tipoFicha.name();

        String sql = "SELECT * FROM posto "
                   + "WHERE estado_admin = 'APROVADO' "
                   + "AND cidade ILIKE ? "
                   + "AND tipo_ficha LIKE ? "
                   + "AND potencia_kw >= ? "
                   + "ORDER BY cidade, codigo";

        List<Posto> postos = new ArrayList<>();

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, filtroCidade);
            ps.setString(2, filtroFicha);
            ps.setDouble(3, potenciaMinima);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                postos.add(lerPosto(rs));
            }
        }
        return postos;
    }

    // estado atual de um posto (o Posto tem os dois estados)
    public Posto consultarEstado(int postoId) throws ErroValidacao, SQLException {

        String sql = "SELECT * FROM posto WHERE id = ?";

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, postoId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                throw new ErroValidacao("O posto #" + postoId + " nao existe.");
            }
            return lerPosto(rs);
        }
    }

    // historico de um posto
    public List<EntradaHistorico> consultarHistorico(int postoId) throws ErroValidacao, SQLException {

        // para dar erro se o posto nao existir
        consultarEstado(postoId);

        String sql = "SELECT * FROM historico WHERE posto_id = ? ORDER BY data_hora, id";

        List<EntradaHistorico> historico = new ArrayList<>();

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, postoId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                historico.add(new EntradaHistorico(
                        rs.getInt("id"),
                        rs.getInt("posto_id"),
                        (Integer) rs.getObject("reserva_id"),
                        rs.getString("tipo"),
                        rs.getString("estado_anterior"),
                        rs.getString("estado_novo"),
                        rs.getString("descricao"),
                        rs.getTimestamp("data_hora").toLocalDateTime()));
            }
        }
        return historico;
    }

    // operacao principal: criar uma reserva
    public Reserva criarReserva(int utilizadorId, int postoId, String textoInicio, String textoFim)
            throws ErroValidacao, SQLException {

        // 1. datas
        LocalDateTime inicio = lerData(textoInicio);
        LocalDateTime fim    = lerData(textoFim);
        validarPeriodo(inicio, fim);

        Connection con = null;
        try {
            con = bd.ligar();
            con.setAutoCommit(false);

            // 2. bloquear utilizador e posto com FOR UPDATE
            // se outro pedido quiser o mesmo posto fica a espera ate ao commit/rollback
            // (sempre pela mesma ordem para nao haver deadlock)

            PreparedStatement psUtil = con.prepareStatement(
                    "SELECT tipo_ficha FROM utilizador WHERE id = ? FOR UPDATE");
            psUtil.setInt(1, utilizadorId);
            ResultSet rsUtil = psUtil.executeQuery();
            if (!rsUtil.next()) {
                throw new ErroValidacao("O utilizador #" + utilizadorId + " nao existe.");
            }
            TipoFicha fichaCarro = TipoFicha.valueOf(rsUtil.getString("tipo_ficha"));

            PreparedStatement psPosto = con.prepareStatement(
                    "SELECT * FROM posto WHERE id = ? FOR UPDATE");
            psPosto.setInt(1, postoId);
            ResultSet rsPosto = psPosto.executeQuery();
            if (!rsPosto.next()) {
                throw new ErroValidacao("O posto #" + postoId + " nao existe.");
            }
            Posto posto = lerPosto(rsPosto);

            // 3. regras de negocio
            if (posto.getEstadoAdmin() != EstadoAdmin.APROVADO) {
                throw new ErroValidacao("O posto " + posto.getCodigo() + " nao esta aprovado.");
            }
            if (!posto.getEstado().aceitaReservas()) {
                throw new ErroValidacao("O posto " + posto.getCodigo() + " esta " + posto.getEstado()
                                        + " e nao aceita reservas.");
            }
            if (posto.getTipoFicha() != fichaCarro) {
                throw new ErroValidacao("Ficha incompativel: o carro usa " + fichaCarro
                                        + " e o posto e " + posto.getTipoFicha() + ".");
            }

            // maximo de reservas ativas
            PreparedStatement psConta = con.prepareStatement(
                    "SELECT COUNT(*) FROM reserva "
                  + "WHERE utilizador_id = ? AND estado = 'ATIVA' AND data_fim > NOW()");
            psConta.setInt(1, utilizadorId);
            ResultSet rsConta = psConta.executeQuery();
            rsConta.next();
            if (rsConta.getInt(1) >= MAX_RESERVAS_ATIVAS) {
                throw new ErroValidacao("O utilizador ja tem " + MAX_RESERVAS_ATIVAS
                                        + " reservas ativas (maximo permitido).");
            }

            // sobreposicao: inicio_existente < fim_novo e fim_existente > inicio_novo
            PreparedStatement psSobre = con.prepareStatement(
                    "SELECT COUNT(*) FROM reserva "
                  + "WHERE posto_id = ? AND estado = 'ATIVA' AND data_inicio < ? AND data_fim > ?");
            psSobre.setInt(1, postoId);
            psSobre.setTimestamp(2, Timestamp.valueOf(fim));
            psSobre.setTimestamp(3, Timestamp.valueOf(inicio));
            ResultSet rsSobre = psSobre.executeQuery();
            rsSobre.next();
            if (rsSobre.getInt(1) > 0) {
                throw new ErroValidacao("O posto " + posto.getCodigo()
                                        + " ja tem uma reserva nesse periodo.");
            }

            // 4. gravar
            PreparedStatement psInsert = con.prepareStatement(
                    "INSERT INTO reserva (utilizador_id, posto_id, data_inicio, data_fim, estado) "
                  + "VALUES (?, ?, ?, ?, 'ATIVA') RETURNING id, criada_em");
            psInsert.setInt(1, utilizadorId);
            psInsert.setInt(2, postoId);
            psInsert.setTimestamp(3, Timestamp.valueOf(inicio));
            psInsert.setTimestamp(4, Timestamp.valueOf(fim));
            ResultSet rsInsert = psInsert.executeQuery();
            rsInsert.next();
            int reservaId = rsInsert.getInt("id");
            LocalDateTime criadaEm = rsInsert.getTimestamp("criada_em").toLocalDateTime();

            // 5. DISPONIVEL -> RESERVADO
            EstadoPosto estadoAntes = posto.getEstado();
            if (estadoAntes.sistemaPodeMudarPara(EstadoPosto.RESERVADO)) {
                PreparedStatement psEstado = con.prepareStatement(
                        "UPDATE posto SET estado = 'RESERVADO' WHERE id = ?");
                psEstado.setInt(1, postoId);
                psEstado.executeUpdate();
            }

            // 6. historico
            Historico.registar(con, postoId, reservaId, "RESERVA_CRIADA",
                    estadoAntes.name(), EstadoPosto.RESERVADO.name(),
                    "Reserva #" + reservaId + " criada pelo utilizador #" + utilizadorId);

            con.commit();

            return new Reserva(reservaId, utilizadorId, postoId, inicio, fim,
                               EstadoReserva.ATIVA, criadaEm);

        } catch (Exception e) {
            if (con != null) {
                con.rollback();
            }
            throw e;

        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // funcionalidade propria: cancelar reserva (so o dono e antes de comecar)
    public void cancelarReserva(int reservaId, int utilizadorId) throws ErroValidacao, SQLException {

        Connection con = null;
        try {
            con = bd.ligar();
            con.setAutoCommit(false);

            PreparedStatement psRes = con.prepareStatement(
                    "SELECT * FROM reserva WHERE id = ?");
            psRes.setInt(1, reservaId);
            ResultSet rsRes = psRes.executeQuery();
            if (!rsRes.next()) {
                throw new ErroValidacao("A reserva #" + reservaId + " nao existe.");
            }
            int postoId = rsRes.getInt("posto_id");

            // bloquear o posto (como no criarReserva)
            PreparedStatement psPosto = con.prepareStatement(
                    "SELECT * FROM posto WHERE id = ? FOR UPDATE");
            psPosto.setInt(1, postoId);
            ResultSet rsPosto = psPosto.executeQuery();
            rsPosto.next();
            Posto posto = lerPosto(rsPosto);

            if (rsRes.getInt("utilizador_id") != utilizadorId) {
                throw new ErroValidacao("A reserva #" + reservaId + " nao pertence ao utilizador #"
                                        + utilizadorId + ".");
            }
            if (!rsRes.getString("estado").equals(EstadoReserva.ATIVA.name())) {
                throw new ErroValidacao("A reserva #" + reservaId + " nao esta ativa (esta "
                                        + rsRes.getString("estado") + ").");
            }
            LocalDateTime inicio = rsRes.getTimestamp("data_inicio").toLocalDateTime();
            if (inicio.isBefore(LocalDateTime.now())) {
                throw new ErroValidacao("A reserva #" + reservaId + " ja comecou e nao pode ser cancelada.");
            }

            PreparedStatement psCancelar = con.prepareStatement(
                    "UPDATE reserva SET estado = 'CANCELADA' WHERE id = ?");
            psCancelar.setInt(1, reservaId);
            psCancelar.executeUpdate();

            // se ficou sem reservas volta a DISPONIVEL
            EstadoPosto estadoAntes = posto.getEstado();
            EstadoPosto estadoDepois = estadoAntes;

            if (contarReservasAtivas(con, postoId) == 0
                    && estadoAntes.sistemaPodeMudarPara(EstadoPosto.DISPONIVEL)) {
                PreparedStatement psEstado = con.prepareStatement(
                        "UPDATE posto SET estado = 'DISPONIVEL' WHERE id = ?");
                psEstado.setInt(1, postoId);
                psEstado.executeUpdate();
                estadoDepois = EstadoPosto.DISPONIVEL;
            }

            Historico.registar(con, postoId, reservaId, "RESERVA_CANCELADA",
                    estadoAntes.name(), estadoDepois.name(),
                    "Reserva #" + reservaId + " cancelada pelo utilizador #" + utilizadorId);

            con.commit();

        } catch (Exception e) {
            if (con != null) {
                con.rollback();
            }
            throw e;

        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // funcionalidade propria: reservas de um utilizador com custo estimado
    public List<Reserva> minhasReservas(int utilizadorId) throws ErroValidacao, SQLException {

        String sql = "SELECT r.*, p.codigo, p.potencia_kw, p.preco_kwh "
                   + "FROM reserva r JOIN posto p ON r.posto_id = p.id "
                   + "WHERE r.utilizador_id = ? "
                   + "ORDER BY r.data_inicio DESC";

        List<Reserva> reservas = new ArrayList<>();

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            PreparedStatement psUtil = con.prepareStatement("SELECT id FROM utilizador WHERE id = ?");
            psUtil.setInt(1, utilizadorId);
            if (!psUtil.executeQuery().next()) {
                throw new ErroValidacao("O utilizador #" + utilizadorId + " nao existe.");
            }

            ps.setInt(1, utilizadorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reserva r = lerReserva(rs);

                double horas = Duration.between(r.getDataInicio(), r.getDataFim()).toMinutes() / 60.0;
                double custo = horas * rs.getDouble("potencia_kw") * rs.getDouble("preco_kwh");

                r.setCodigoPosto(rs.getString("codigo"));
                r.setCustoEstimado(custo);
                reservas.add(r);
            }
        }
        return reservas;
    }

    // funcionalidade propria: postos livres num periodo, com a ficha do carro
    public List<Posto> procurarPostosLivres(int utilizadorId, String textoInicio, String textoFim)
            throws ErroValidacao, SQLException {

        LocalDateTime inicio = lerData(textoInicio);
        LocalDateTime fim    = lerData(textoFim);
        validarPeriodo(inicio, fim);

        String sql = "SELECT * FROM posto p "
                   + "WHERE p.estado_admin = 'APROVADO' "
                   + "AND p.estado IN ('DISPONIVEL', 'RESERVADO') "
                   + "AND p.tipo_ficha = ? "
                   + "AND NOT EXISTS ("
                   + "    SELECT 1 FROM reserva r "
                   + "    WHERE r.posto_id = p.id AND r.estado = 'ATIVA' "
                   + "    AND r.data_inicio < ? AND r.data_fim > ?"
                   + ") "
                   + "ORDER BY p.cidade, p.codigo";

        List<Posto> postos = new ArrayList<>();

        try (Connection con = bd.ligar()) {

            PreparedStatement psUtil = con.prepareStatement("SELECT tipo_ficha FROM utilizador WHERE id = ?");
            psUtil.setInt(1, utilizadorId);
            ResultSet rsUtil = psUtil.executeQuery();
            if (!rsUtil.next()) {
                throw new ErroValidacao("O utilizador #" + utilizadorId + " nao existe.");
            }
            String ficha = rsUtil.getString("tipo_ficha");

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, ficha);
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            ps.setTimestamp(3, Timestamp.valueOf(inicio));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                postos.add(lerPosto(rs));
            }
        }
        return postos;
    }

    // tambem usado pelo ServicoAdmin ao editar
    public static void validarDadosPosto(String codigo, String localizacao, String cidade,
                                         double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws ErroValidacao {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ErroValidacao("O codigo e obrigatorio.");
        }
        if (localizacao == null || localizacao.trim().isEmpty()) {
            throw new ErroValidacao("A localizacao e obrigatoria.");
        }
        if (cidade == null || cidade.trim().isEmpty()) {
            throw new ErroValidacao("A cidade e obrigatoria.");
        }
        if (potenciaKw <= 0) {
            throw new ErroValidacao("A potencia tem de ser maior que 0.");
        }
        if (tipoFicha == null) {
            throw new ErroValidacao("O tipo de ficha e obrigatorio.");
        }
        if (precoKwh < 0) {
            throw new ErroValidacao("O preco nao pode ser negativo.");
        }
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) throws ErroValidacao {
        if (!fim.isAfter(inicio)) {
            throw new ErroValidacao("A data de fim tem de ser depois da data de inicio.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ErroValidacao("Nao e possivel reservar um periodo que ja comecou ou ja passou.");
        }
        if (Duration.between(inicio, fim).toMinutes() > MAX_HORAS * 60) {
            throw new ErroValidacao("Uma reserva nao pode durar mais de " + MAX_HORAS + " horas.");
        }
    }

    private int contarReservasAtivas(Connection con, int postoId) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM reserva "
              + "WHERE posto_id = ? AND estado = 'ATIVA' AND data_fim > NOW()");
        ps.setInt(1, postoId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public static Reserva lerReserva(ResultSet rs) throws SQLException {
        return new Reserva(
                rs.getInt("id"),
                rs.getInt("utilizador_id"),
                rs.getInt("posto_id"),
                rs.getTimestamp("data_inicio").toLocalDateTime(),
                rs.getTimestamp("data_fim").toLocalDateTime(),
                EstadoReserva.valueOf(rs.getString("estado")),
                rs.getTimestamp("criada_em").toLocalDateTime());
    }

    private LocalDateTime lerData(String texto) throws ErroValidacao {
        if (texto == null) {
            throw new ErroValidacao("Data em falta.");
        }
        try {
            return LocalDateTime.parse(texto.trim(), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw new ErroValidacao("Data mal formada: '" + texto + "'. Use o formato AAAA-MM-DD HH:MM.");
        }
    }

    // linha do ResultSet -> Posto (tambem usado no ServicoAdmin)
    public static Posto lerPosto(ResultSet rs) throws SQLException {
        return new Posto(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("localizacao"),
                rs.getString("cidade"),
                rs.getDouble("potencia_kw"),
                TipoFicha.valueOf(rs.getString("tipo_ficha")),
                rs.getDouble("preco_kwh"),
                EstadoPosto.valueOf(rs.getString("estado")),
                EstadoAdmin.valueOf(rs.getString("estado_admin")));
    }
}
