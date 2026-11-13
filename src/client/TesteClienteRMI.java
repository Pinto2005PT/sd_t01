package client;

import common.Config;
import common.ErroValidacao;
import common.Posto;
import common.ServicoGeralRMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

/** Programa de teste (temporario): liga-se ao servidor por RMI e chama 2 metodos. */
public class TesteClienteRMI {

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        String host = config.getProperty("rmi.host");
        int porta = Integer.parseInt(config.getProperty("rmi.port"));
        String nome = config.getProperty("rmi.nome");

        // 1. Encontrar o registo RMI do servidor e pedir o servico pelo nome
        Registry registo = LocateRegistry.getRegistry(host, porta);
        ServicoGeralRMI servico = (ServicoGeralRMI) registo.lookup(nome);
        System.out.println("Ligado ao servidor RMI!\n");

        // 2. Chamar metodos remotos como se fossem locais
        System.out.println("=== Postos aprovados (pedidos ao servidor por RMI) ===");
        for (Posto p : servico.listarPostos("", null, 0)) {
            System.out.println(p);
        }

        System.out.println("\n=== Erro de validacao que vem do servidor ===");
        try {
            servico.consultarEstado(999);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }
    }
}
