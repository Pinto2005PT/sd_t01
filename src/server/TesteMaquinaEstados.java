package server;

import common.EstadoPosto;

/** Programa de teste (temporario): mostra que transicoes o admin pode fazer. */
public class TesteMaquinaEstados {

    public static void main(String[] args) {
        System.out.println("Transicoes permitidas ao ADMIN:\n");
        for (EstadoPosto de : EstadoPosto.values()) {
            for (EstadoPosto para : EstadoPosto.values()) {
                if (de == para) continue;
                String resultado = de.adminPodeMudarPara(para) ? "OK" : "rejeitada";
                System.out.printf("%-14s -> %-14s : %s%n", de, para, resultado);
            }
            System.out.println();
        }

        System.out.println("Aceita reservas?");
        for (EstadoPosto e : EstadoPosto.values()) {
            System.out.printf("%-14s : %s%n", e, e.aceitaReservas() ? "sim" : "nao");
        }
    }
}
