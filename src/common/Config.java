package common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

// Le o ficheiro de configuracao (config.properties).
public class Config {

    public static Properties carregar(String ficheiro) throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(ficheiro)) {
            props.load(in);
        }
        return props;
    }
}
