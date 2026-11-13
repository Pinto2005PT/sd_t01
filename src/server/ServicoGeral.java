package server;

import common.ErroValidacao;
import common.EstadoAdmin;
import common.EstadoPosto;
import common.Posto;
import common.TipoFicha;
import common.Utilizador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
