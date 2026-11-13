package server;

import common.Config;
import common.ErroValidacao;
import common.Reserva;

import java.time.LocalDate;
import java.util.Properties;

/** Programa de teste (temporario) para o criarReserva - passo 4c. */
public class TesteReservas {

    static ServicoGeral servico;

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        servico = new ServicoGeral(new BaseDados(config));

        String amanha = LocalDate.now().plusDays(1).toString();   // ex.: "2026-11-14"
        String depois = LocalDate.now().plusDays(2).toString();

        // ---- Casos que TEM de ser rejeitados ----
        tentar("Data mal formada",           1, 3, "amanha as 3", amanha + " 10:00");
        tentar("Fim antes do inicio",        1, 3, amanha + " 12:00", amanha + " 11:00");
        tentar("Periodo ja passou",          1, 3, "2020-01-01 10:00", "2020-01-01 11:00");
        tentar("Mais de 2 horas",            1, 3, amanha + " 10:00", amanha + " 13:00");
        tentar("Utilizador inexistente",    99, 3, amanha + " 10:00", amanha + " 11:00");
        tentar("Posto inexistente",          1, 99, amanha + " 10:00", amanha + " 11:00");
        tentar("Posto nao aprovado (LSB-002)", 2, 6, amanha + " 10:00", amanha + " 11:00");
        tentar("Posto em manutencao (EVR-004)", 3, 4, amanha + " 10:00", amanha + " 11:00");
        tentar("Ficha incompativel (Pedro CHADEMO no EVR-003 CCS)", 3, 3, amanha + " 10:00", amanha + " 11:00");
        tentar("Sobreposicao (EVR-001 ja reservado 14h-15h)", 4, 1, amanha + " 14:30", amanha + " 15:30");

        // ---- Casos validos ----
        tentar("Reserva valida (Guilherme Pinto, EVR-003)", 1, 3, amanha + " 10:00", amanha + " 11:00");
        tentar("Mais de 2 ativas (Guilherme Pinto ja tem 2)", 1, 5, depois + " 10:00", depois + " 11:00");

        // ---- CONCORRENCIA: 2 clientes a reservar o MESMO posto e periodo AO MESMO TEMPO ----
        System.out.println("\n=== Concorrencia: 2 pedidos simultaneos para o LSB-001 ===");
        Thread t1 = new ClienteSimulado("Cliente A", 4, 5, depois + " 18:00", depois + " 19:00");
        Thread t2 = new ClienteSimulado("Cliente B", 5, 5, depois + " 18:30", depois + " 19:30");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("(so um dos dois pode ter conseguido)");
    }

    /** Tenta criar uma reserva e mostra o resultado. */
    static void tentar(String descricao, int utilizador, int posto, String inicio, String fim) {
        try {
            Reserva r = servico.criarReserva(utilizador, posto, inicio, fim);
            System.out.println("[ACEITE]    " + descricao + " -> " + r);
        } catch (ErroValidacao e) {
            System.out.println("[REJEITADO] " + descricao + " -> " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERRO BD]   " + descricao + " -> " + e.getMessage());
        }
    }

    /** Uma thread que simula um cliente a fazer uma reserva. */
    static class ClienteSimulado extends Thread {
        String nome, inicio, fim;
        int utilizador, posto;

        ClienteSimulado(String nome, int utilizador, int posto, String inicio, String fim) {
            this.nome = nome;
            this.utilizador = utilizador;
            this.posto = posto;
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public void run() {
            tentar(nome, utilizador, posto, inicio, fim);
        }
    }
}
