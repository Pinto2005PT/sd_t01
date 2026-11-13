package server;

import common.Config;
import common.ErroValidacao;
import common.Posto;
import common.TipoFicha;
import common.Utilizador;

import java.util.Properties;

/** Programa de teste (temporario) para o ServicoGeral. */
public class TesteServicoGeral {

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        ServicoGeral servico = new ServicoGeral(new BaseDados(config));

        // 1. Registo valido
        try {
            Utilizador u = servico.registarUtilizador("Rui Teste", "rui@mail.pt", "99-ZZ-99", TipoFicha.CCS);
            System.out.println("OK  utilizador criado: " + u);
        } catch (ErroValidacao e) {
            System.out.println("ERRO: " + e.getMessage());
        }

        // 2. Email repetido (tem de ser rejeitado)
        try {
            servico.registarUtilizador("Outro", "rui@mail.pt", "11-AA-11", TipoFicha.CCS);
            System.out.println("MAL: devia ter sido rejeitado!");
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        // 3. Posto valido
        try {
            Posto p = servico.registarPosto("FAR-001", "Marina de Faro", "Faro", 50, TipoFicha.CCS, 0.40);
            System.out.println("OK  posto criado: " + p);
        } catch (ErroValidacao e) {
            System.out.println("ERRO: " + e.getMessage());
        }

        // 4. Potencia invalida (tem de ser rejeitado)
        try {
            servico.registarPosto("FAR-002", "Centro", "Faro", -10, TipoFicha.CCS, 0.40);
            System.out.println("MAL: devia ter sido rejeitado!");
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }
    }
}
