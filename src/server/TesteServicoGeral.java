package server;

import common.Config;
import common.EntradaHistorico;
import common.ErroValidacao;
import common.Posto;
import common.TipoFicha;

import java.util.List;
import java.util.Properties;

/** Programa de teste (temporario) para o ServicoGeral - passo 4b. */
public class TesteServicoGeral {

    public static void main(String[] args) throws Exception {
        Properties config = Config.carregar("config.properties");
        ServicoGeral servico = new ServicoGeral(new BaseDados(config));

        System.out.println("=== 1. Todos os postos aprovados (sem filtros) ===");
        for (Posto p : servico.listarPostos("", null, 0)) {
            System.out.println(p);
        }

        System.out.println("\n=== 2. Filtro: cidade 'evora', ficha CCS ===");
        for (Posto p : servico.listarPostos("evora", TipoFicha.CCS, 0)) {
            System.out.println(p);
        }

        System.out.println("\n=== 3. Filtro: potencia minima 100 kW ===");
        for (Posto p : servico.listarPostos("", null, 100)) {
            System.out.println(p);
        }

        System.out.println("\n=== 4. Estado atual do posto 4 ===");
        Posto p4 = servico.consultarEstado(4);
        System.out.println(p4.getCodigo() + ": operacional = " + p4.getEstado()
                           + ", administrativo = " + p4.getEstadoAdmin());

        System.out.println("\n=== 5. Estado de um posto que nao existe (tem de dar erro) ===");
        try {
            servico.consultarEstado(999);
        } catch (ErroValidacao e) {
            System.out.println("OK  rejeitado: " + e.getMessage());
        }

        System.out.println("\n=== 6. Historico do posto 1 ===");
        List<EntradaHistorico> historico = servico.consultarHistorico(1);
        for (EntradaHistorico h : historico) {
            System.out.println(h);
        }
    }
}
