package server;

import common.Config;
import common.ErroValidacao;
import common.Posto;
import common.Reserva;

import java.time.LocalDate;
import java.util.Properties;

/** Programa de teste (temporario) para as funcionalidades proprias - passo 4d. */
public class TesteProprias {

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        ServicoGeral servico = new ServicoGeral(new BaseDados(config));

        String amanha = LocalDate.now().plusDays(1).toString();

        System.out.println("=== 1. Minhas reservas (Guilherme Pinto, #1) ===");
        for (Reserva r : servico.minhasReservas(1)) {
            System.out.println(r);
        }

        System.out.println("\n=== 2. Postos livres amanha 14:00-15:00 para a Ana (#4, CCS) ===");
        System.out.println("(o EVR-001 NAO pode aparecer: ja esta reservado a essa hora)");
        for (Posto p : servico.procurarPostosLivres(4, amanha + " 14:00", amanha + " 15:00")) {
            System.out.println(p);
        }

        System.out.println("\n=== 3. Cancelar reserva de outro utilizador (tem de dar erro) ===");
        try {
            servico.cancelarReserva(1, 4);   // a reserva #1 e do Guilherme Pinto, nao da Ana
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        System.out.println("\n=== 4. Cancelar reserva ja concluida (tem de dar erro) ===");
        try {
            servico.cancelarReserva(3, 2);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        System.out.println("\n=== 5. Cancelar as reservas #1 e #2 do EVR-001 ===");
        servico.cancelarReserva(1, 1);
        System.out.println("Reserva #1 cancelada. Estado do EVR-001: " + servico.consultarEstado(1).getEstado());
        servico.cancelarReserva(2, 4);
        System.out.println("Reserva #2 cancelada. Estado do EVR-001: " + servico.consultarEstado(1).getEstado()
                           + "  (ja nao tem reservas -> DISPONIVEL)");

        System.out.println("\n=== 6. Postos livres outra vez (agora o EVR-001 ja aparece) ===");
        for (Posto p : servico.procurarPostosLivres(4, amanha + " 14:00", amanha + " 15:00")) {
            System.out.println(p);
        }
    }
}
