package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Escreve uma linha na tabela historico.
 * Usado pelo ServicoGeral e pelo ServicoAdmin.
 *
 * Recebe a Connection de quem chama, para a escrita no historico
 * ficar DENTRO da mesma transacao da operacao (ou grava tudo, ou nada).
 */
public class Historico {

    public static void registar(Connection con, int postoId, Integer reservaId, String tipo,
                                String estadoAnterior, String estadoNovo, String descricao)
            throws SQLException {

        String sql = "INSERT INTO historico (posto_id, reserva_id, tipo, estado_anterior, estado_novo, descricao) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postoId);

            // reserva_id pode ser null (ex.: registo ou aprovacao de um posto)
            if (reservaId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, reservaId);
            }

            ps.setString(3, tipo);
            ps.setString(4, estadoAnterior);
            ps.setString(5, estadoNovo);
            ps.setString(6, descricao);
            ps.executeUpdate();
        }
    }
}
