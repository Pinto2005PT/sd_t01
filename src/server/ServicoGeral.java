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

/**
 * Logica do servidor para as funcionalidades do CLIENTE GERAL.
 * Valida os pedidos e le/escreve na base de dados.
 */
public class ServicoGeral {

    // Codigo de erro do PostgreSQL quando se viola um UNIQUE (valor repetido)
    private static final String ERRO_REPETIDO = "23505";

    // Regras de negocio da reserva
    private static final int MAX_RESERVAS_ATIVAS = 2;
    private static final int MAX_HORAS = 2;

    // Formato das datas que o cliente envia, ex.: "2026-11-20 14:00"
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BaseDados bd;

    public ServicoGeral(BaseDados bd) {
        this.bd = bd;
    }

    // =====================================================
    // REGISTO DE UTILIZADOR
    // =====================================================
    public Utilizador registarUtilizador(String nome, String email, String matricula, TipoFicha tipoFicha)
            throws ErroValidacao, SQLException {

        // 1. Validar os dados
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

        // 2. Inserir na BD. O "RETURNING id" devolve o id gerado pelo SERIAL.
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

    // =====================================================
    // REGISTO DE POSTO (fica PENDENTE ate o admin aprovar)
    // =====================================================
    public Posto registarPosto(String codigo, String localizacao, String cidade,
                               double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws ErroValidacao, SQLException {

        // 1. Validar os dados
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

        // O posto novo comeca sempre DISPONIVEL e PENDENTE
        Posto posto = new Posto(codigo.trim(), localizacao.trim(), cidade.trim(),
                                potenciaKw, tipoFicha, precoKwh);

        String sql = "INSERT INTO posto (codigo, localizacao, cidade, potencia_kw, tipo_ficha, "
                   + "preco_kwh, estado, estado_admin) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        Connection con = null;
        try {
            con = bd.ligar();

            // 2. TRANSACAO: inserir o posto + escrever no historico.
            //    Ou ficam as duas coisas gravadas, ou nenhuma.
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

            con.commit();   // grava tudo

            return new Posto(id, posto.getCodigo(), posto.getLocalizacao(), posto.getCidade(),
                             posto.getPotenciaKw(), posto.getTipoFicha(), posto.getPrecoKwh(),
                             EstadoPosto.DISPONIVEL, EstadoAdmin.PENDENTE);

        } catch (SQLException e) {
            if (con != null) {
                con.rollback();   // desfaz tudo o que foi feito nesta transacao
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

    // =====================================================
    // LISTAGEM COM FILTROS (so postos APROVADOS)
    // Filtros: cidade, tipo de ficha e potencia minima.
    // Um filtro vazio/null significa "qualquer valor".
    // =====================================================
    public List<Posto> listarPostos(String cidade, TipoFicha tipoFicha, double potenciaMinima)
            throws SQLException {

        // Truque simples: se o filtro estiver vazio usamos '%', que no LIKE quer dizer "qualquer coisa".
        String filtroCidade = (cidade == null || cidade.trim().isEmpty()) ? "%" : cidade.trim();
        String filtroFicha  = (tipoFicha == null) ? "%" : tipoFicha.name();

        String sql = "SELECT * FROM posto "
                   + "WHERE estado_admin = 'APROVADO' "
                   + "AND cidade ILIKE ? "          // ILIKE = LIKE sem ligar a maiusculas/minusculas
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

    // =====================================================
    // CONSULTA DE ESTADO ATUAL (operacional + administrativo)
    // Devolve o Posto, que tem os dois estados.
    // =====================================================
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

    // =====================================================
    // CONSULTA DE HISTORICO de um posto (do mais antigo para o mais recente)
    // =====================================================
    public List<EntradaHistorico> consultarHistorico(int postoId) throws ErroValidacao, SQLException {

        // Primeiro confirma que o posto existe (senao lanca ErroValidacao)
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
                        (Integer) rs.getObject("reserva_id"),   // pode vir null
                        rs.getString("tipo"),
                        rs.getString("estado_anterior"),
                        rs.getString("estado_novo"),
                        rs.getString("descricao"),
                        rs.getTimestamp("data_hora").toLocalDateTime()));
            }
        }
        return historico;
    }

    // =====================================================
    // OPERACAO PRINCIPAL: CRIAR RESERVA
    // As datas chegam como texto "yyyy-MM-dd HH:mm" e sao validadas aqui no servidor.
    // =====================================================
    public Reserva criarReserva(int utilizadorId, int postoId, String textoInicio, String textoFim)
            throws ErroValidacao, SQLException {

        // ---------- 1. Validar as datas (nao precisa da BD) ----------
        LocalDateTime inicio = lerData(textoInicio);
        LocalDateTime fim    = lerData(textoFim);

        if (!fim.isAfter(inicio)) {
            throw new ErroValidacao("A data de fim tem de ser depois da data de inicio.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ErroValidacao("Nao e possivel reservar um periodo que ja comecou ou ja passou.");
        }
        if (Duration.between(inicio, fim).toMinutes() > MAX_HORAS * 60) {
            throw new ErroValidacao("Uma reserva nao pode durar mais de " + MAX_HORAS + " horas.");
        }

        Connection con = null;
        try {
            con = bd.ligar();
            con.setAutoCommit(false);   // inicio da TRANSACAO

            // ---------- 2. Bloquear o utilizador e o posto (CONCORRENCIA) ----------
            // "FOR UPDATE" bloqueia a linha ate ao commit/rollback.
            // Se outro cliente tentar reservar o mesmo posto ao mesmo tempo,
            // fica a ESPERAR aqui ate esta transacao acabar.
            // Bloqueamos sempre pela mesma ordem (utilizador, depois posto) para evitar deadlocks.

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

            // ---------- 3. Regras de negocio (com o estado ATUAL da BD) ----------
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

            // Maximo de reservas ativas (futuras) por utilizador
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

            // Sobreposicao: existe outra reserva ATIVA neste posto que se cruze com [inicio, fim]?
            // Dois periodos cruzam-se se: inicio_existente < fim_novo  E  fim_existente > inicio_novo
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

            // ---------- 4. Tudo valido: gravar a reserva ----------
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

            // ---------- 5. Mudar o estado do posto (DISPONIVEL -> RESERVADO) ----------
            EstadoPosto estadoAntes = posto.getEstado();
            if (estadoAntes.sistemaPodeMudarPara(EstadoPosto.RESERVADO)) {
                PreparedStatement psEstado = con.prepareStatement(
                        "UPDATE posto SET estado = 'RESERVADO' WHERE id = ?");
                psEstado.setInt(1, postoId);
                psEstado.executeUpdate();
            }

            // ---------- 6. Historico ----------
            Historico.registar(con, postoId, reservaId, "RESERVA_CRIADA",
                    estadoAntes.name(), EstadoPosto.RESERVADO.name(),
                    "Reserva #" + reservaId + " criada pelo utilizador #" + utilizadorId);

            con.commit();   // grava tudo e liberta os bloqueios

            return new Reserva(reservaId, utilizadorId, postoId, inicio, fim,
                               EstadoReserva.ATIVA, criadaEm);

        } catch (ErroValidacao | SQLException e) {
            // Qualquer erro: desfaz tudo e liberta os bloqueios
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

    // =====================================================
    // AUXILIAR: converte "yyyy-MM-dd HH:mm" em LocalDateTime
    // =====================================================
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

    // =====================================================
    // AUXILIAR: converte a linha atual do ResultSet num objeto Posto.
    // (static para o ServicoAdmin tambem poder usar: ServicoGeral.lerPosto(rs))
    // =====================================================
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
