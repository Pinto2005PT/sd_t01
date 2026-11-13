package server;

import common.Config;
import common.ErroValidacao;
import common.EstadoAdmin;
import common.Posto;
import common.TipoFicha;

import java.util.Properties;

/** Programa de teste (temporario) para o ServicoAdmin - passo B1. */
public class TesteServicoAdmin {

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        ServicoAdmin admin = new ServicoAdmin(new BaseDados(config));

        System.out.println("=== 1. Postos PENDENTES ===");
        for (Posto p : admin.listarPorEstadoAdmin(EstadoAdmin.PENDENTE)) {
            System.out.println(p);
        }

        System.out.println("\n=== 2. Aprovar o LSB-002 (#6) e rejeitar o BJA-001 (#7) ===");
        System.out.println("Aprovado:  " + admin.aprovarPosto(6));
        System.out.println("Rejeitado: " + admin.rejeitarPosto(7));

        System.out.println("\n=== 3. Aprovar outra vez o #6 (tem de dar erro) ===");
        try {
            admin.aprovarPosto(6);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        System.out.println("\n=== 4. Postos PENDENTES agora (lista vazia) ===");
        System.out.println(admin.listarPorEstadoAdmin(EstadoAdmin.PENDENTE).size() + " postos");

        System.out.println("\n=== 5. Editar o EVR-002 (#2): novo preco 0.30 ===");
        System.out.println(admin.editarPosto(2, "EVR-002", "Praca do Giraldo", "Evora", 22, TipoFicha.TIPO2, 0.30));

        System.out.println("\n=== 6. Mudar a ficha do EVR-001 (#1, tem reservas) - tem de dar erro ===");
        try {
            admin.editarPosto(1, "EVR-001", "Parque do Rossio", "Evora", 50, TipoFicha.TIPO2, 0.35);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        System.out.println("\n=== 7. Editar com codigo repetido - tem de dar erro ===");
        try {
            admin.editarPosto(2, "EVR-001", "Praca do Giraldo", "Evora", 22, TipoFicha.TIPO2, 0.30);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }
    }
}
