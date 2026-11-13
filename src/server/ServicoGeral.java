package server;

import common.ErroValidacao;
import common.EstadoAdmin;
import common.EstadoPosto;
import common.Posto;
import common.TipoFicha;
import common.Utilizador;

import common.EntradaHistorico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica do servidor para as funcionalidades do CLIENTE GERAL.
 * Valida os pedidos e le/escreve na base de dados.
 */
public class ServicoGeral {

    // Codigo de erro do PostgreSQL quando se viola um UNIQUE (valor repetido)
    private static final String ERRO_REPETIDO = "23505";

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
