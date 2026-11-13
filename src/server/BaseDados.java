package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Guarda os dados de acesso a BD e abre ligacoes.
public class BaseDados {

    private final String url;
    private final String user;
    private final String password;

    public BaseDados(Properties config) {
        this.url      = config.getProperty("db.url");
        this.user     = config.getProperty("db.user");
        this.password = config.getProperty("db.password");
    }

    public Connection ligar() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
