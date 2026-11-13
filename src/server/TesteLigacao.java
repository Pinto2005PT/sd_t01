package server;

import common.Config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Programa de teste (temporario): liga-se a BD e lista os postos.
 */
public class TesteLigacao {

    public static void main(String[] args) {
        String ficheiro = (args.length > 0) ? args[0] : "config.properties";

        // 1. Ler a configuracao
        Properties config;
        try {
            config = Config.carregar(ficheiro);
        } catch (IOException e) {
            System.out.println("Erro: nao consegui ler o ficheiro " + ficheiro);
            return;
        }

        BaseDados bd = new BaseDados(config);

        // 2. Ligar a BD, fazer uma query e mostrar o resultado
        try (Connection con = bd.ligar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, codigo, cidade, estado, estado_admin FROM posto ORDER BY id")) {

            System.out.println("Ligacao a BD feita com sucesso!\n");

            while (rs.next()) {
                System.out.printf("%d | %-8s | %-7s | %-14s | %s%n",
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("cidade"),
                        rs.getString("estado"),
                        rs.getString("estado_admin"));
            }

        } catch (SQLException e) {
            System.out.println("Erro ao ligar a base de dados: " + e.getMessage());
        }
    }
}
