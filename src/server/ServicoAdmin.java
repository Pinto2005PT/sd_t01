package server;

import common.ErroValidacao;
import common.EstadoAdmin;
import common.Posto;
import common.TipoFicha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Logica do servidor para o cliente administrador
public class ServicoAdmin {

    // erro do PostgreSQL quando um valor UNIQUE ja existe
    private static final String ERRO_REPETIDO = "23505";

    private final BaseDados bd;

    public ServicoAdmin(BaseDados bd) {
        this.bd = bd;
    }

    // listagem por estado administrativo
    public List<Posto> listarPorEstadoAdmin(EstadoAdmin estadoAdmin) throws ErroValidacao, SQLException {

        if (estadoAdmin == null) {
            throw new ErroValidacao("Indique o estado administrativo.");
        }

        String sql = "SELECT * FROM posto WHERE estado_admin = ? ORDER BY id";
        List<Posto> postos = new ArrayList<>();

        try (Connection con = bd.ligar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estadoAdmin.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                postos.add(ServicoGeral.lerPosto(rs));
            }
        }
        return postos;
    }

    // aprovar / rejeitar (so postos PENDENTES)
    public Posto aprovarPosto(int postoId) throws ErroValidacao, SQLException {
        return decidirAprovacao(postoId, EstadoAdmin.APROVADO, "APROVACAO", "Posto aprovado pelo administrador");
    }

    public Posto rejeitarPosto(int postoId) throws ErroValidacao, SQLException {
        return decidirAprovacao(postoId, EstadoAdmin.REJEITADO, "REJEICAO", "Posto rejeitado pelo administrador");
    }

    private Posto decidirAprovacao(int postoId, EstadoAdmin novoEstado, String tipoHistorico, String descricao)
            throws ErroValidacao, SQLException {

        Connection con = null;
        try {
            con = bd.ligar();
            con.setAutoCommit(false);

            Posto posto = lerPostoBloqueado(con, postoId);

            if (posto.getEstadoAdmin() != EstadoAdmin.PENDENTE) {
                throw new ErroValidacao("O posto " + posto.getCodigo() + " nao esta pendente (esta "
                                        + posto.getEstadoAdmin() + ").");
            }

            PreparedStatement ps = con.prepareStatement("UPDATE posto SET estado_admin = ? WHERE id = ?");
            ps.setString(1, novoEstado.name());
            ps.setInt(2, postoId);
            ps.executeUpdate();

            Historico.registar(con, postoId, null, tipoHistorico,
                               EstadoAdmin.PENDENTE.name(), novoEstado.name(), descricao);

            con.commit();

            return new Posto(posto.getId(), posto.getCodigo(), posto.getLocalizacao(), posto.getCidade(),
                             posto.getPotenciaKw(), posto.getTipoFicha(), posto.getPrecoKwh(),
                             posto.getEstado(), novoEstado);

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

    // editar atributos de um posto
    public Posto editarPosto(int postoId, String codigo, String localizacao, String cidade,
                             double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws ErroValidacao, SQLException {

        ServicoGeral.validarDadosPosto(codigo, localizacao, cidade, potenciaKw, tipoFicha, precoKwh);

        Connection con = null;
        try {
            con = bd.ligar();
            con.setAutoCommit(false);

            Posto posto = lerPostoBloqueado(con, postoId);

            // nao se muda a ficha se houver reservas (os carros deixavam de ser compativeis)
            if (posto.getTipoFicha() != tipoFicha && contarReservasAtivas(con, postoId) > 0) {
                throw new ErroValidacao("Nao e possivel mudar a ficha do posto " + posto.getCodigo()
                                        + ": tem reservas ativas.");
            }

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE posto SET codigo = ?, localizacao = ?, cidade = ?, potencia_kw = ?, "
                  + "tipo_ficha = ?, preco_kwh = ? WHERE id = ?");
            ps.setString(1, codigo.trim());
            ps.setString(2, localizacao.trim());
            ps.setString(3, cidade.trim());
            ps.setDouble(4, potenciaKw);
            ps.setString(5, tipoFicha.name());
            ps.setDouble(6, precoKwh);
            ps.setInt(7, postoId);
            ps.executeUpdate();

            Historico.registar(con, postoId, null, "EDICAO",
                               posto.getEstado().name(), posto.getEstado().name(),
                               "Atributos editados pelo administrador");

            con.commit();

            return new Posto(postoId, codigo.trim(), localizacao.trim(), cidade.trim(),
                             potenciaKw, tipoFicha, precoKwh, posto.getEstado(), posto.getEstadoAdmin());

        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            if (ERRO_REPETIDO.equals(e.getSQLState())) {
                throw new ErroValidacao("Ja existe um posto com o codigo " + codigo + ".");
            }
            throw e;

        } catch (ErroValidacao e) {
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

    // le o posto e bloqueia-o ate ao fim da transacao (igual ao criarReserva)
    private Posto lerPostoBloqueado(Connection con, int postoId) throws ErroValidacao, SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM posto WHERE id = ? FOR UPDATE");
        ps.setInt(1, postoId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            throw new ErroValidacao("O posto #" + postoId + " nao existe.");
        }
        return ServicoGeral.lerPosto(rs);
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
}
