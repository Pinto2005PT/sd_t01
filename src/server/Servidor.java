package server;

import common.Config;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

/**
 * Programa principal do servidor.
 * Arranca a parte RMI (cliente geral) e a parte de sockets (cliente admin).
 *
 * Uso: java server.Servidor config.properties
 */
public class Servidor {

    public static void main(String[] args) {
        String ficheiro = (args.length > 0) ? args[0] : "config.properties";

        try {
            // 1. Ler a configuracao
            Properties config = Config.carregar(ficheiro);
            BaseDados bd = new BaseDados(config);

            // 2. RMI (cliente geral)
            int portaRMI = Integer.parseInt(config.getProperty("rmi.port"));
            String nomeRMI = config.getProperty("rmi.nome");

            ServicoGeral servicoGeral = new ServicoGeral(bd);
            ServicoGeralRMIImpl servicoRMI = new ServicoGeralRMIImpl(servicoGeral);

            // Cria o registo RMI dentro do proprio servidor (nao e preciso correr o rmiregistry)
            Registry registo = LocateRegistry.createRegistry(portaRMI);
            // Regista o servico com um nome, para os clientes o poderem encontrar
            registo.rebind(nomeRMI, servicoRMI);

            System.out.println("[Servidor] RMI pronto: '" + nomeRMI + "' na porta " + portaRMI);

            // 3. Sockets (cliente admin)
            // TODO (Guilherme Santos): arrancar aqui o servidor de sockets numa thread

            System.out.println("[Servidor] A aguardar clientes...");

        } catch (Exception e) {
            System.out.println("[Servidor] Erro ao arrancar: " + e.getMessage());
        }
    }
}
