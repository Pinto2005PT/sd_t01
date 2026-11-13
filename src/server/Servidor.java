package server;

import common.Config;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

// Programa principal do servidor.
public class Servidor {

    public static void main(String[] args) {
        String ficheiro = (args.length > 0) ? args[0] : "config.properties";

        try {
            // 1. configuracao
            Properties config = Config.carregar(ficheiro);
            BaseDados bd = new BaseDados(config);

            // 2. RMI (cliente geral)
            String hostRMI = config.getProperty("rmi.host");
            int portaRMI = Integer.parseInt(config.getProperty("rmi.port"));
            String nomeRMI = config.getProperty("rmi.nome");

            ServicoGeral servicoGeral = new ServicoGeral(bd);
            ServicoGeralRMIImpl servicoRMI = new ServicoGeralRMIImpl(servicoGeral);

            // cria o registry aqui (assim nao e preciso correr o rmiregistry a parte)
            LocateRegistry.createRegistry(portaRMI);
            Naming.rebind("//" + hostRMI + ":" + portaRMI + "/" + nomeRMI, servicoRMI);

            System.out.println("[Servidor] RMI pronto: " + nomeRMI + " na porta " + portaRMI);

            // 3. Sockets (cliente admin)
            // TODO (Guilherme Santos): arrancar aqui o servidor de sockets numa thread

            System.out.println("[Servidor] A aguardar clientes...");

        } catch (Exception e) {
            System.out.println("[Servidor] Erro ao arrancar: " + e.getMessage());
        }
    }
}
