package client;

import common.Config;
import common.EntradaHistorico;
import common.ErroValidacao;
import common.Posto;
import common.Reserva;
import common.ServicoGeralRMI;
import common.TipoFicha;
import common.Utilizador;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

// Cliente geral (condutores). Comunica com o servidor por RMI.
public class ClienteGeral {

    private static Scanner teclado = new Scanner(System.in);

    private static String host;
    private static int porta;
    private static String nome;

    // null = nao esta ligado
    private static ServicoGeralRMI servico = null;

    public static void main(String[] args) {
        String ficheiro = (args.length > 0) ? args[0] : "config.properties";

        try {
            Properties config = Config.carregar(ficheiro);
            host  = config.getProperty("rmi.host");
            porta = Integer.parseInt(config.getProperty("rmi.port"));
            nome  = config.getProperty("rmi.nome");
        } catch (Exception e) {
            System.out.println("Erro: nao consegui ler o ficheiro de configuracao " + ficheiro);
            return;
        }

        ligar();

        int opcao = -1;
        while (opcao != 0) {
            mostrarMenu();
            opcao = lerInteiro("Opcao: ");

            if (opcao == 0) {
                break;
            }

            // se o servidor tinha caido tenta ligar outra vez
            if (servico == null && !ligar()) {
                continue;
            }

            try {
                executar(opcao);

            } catch (ErroValidacao e) {
                System.out.println("\n>> Pedido rejeitado: " + e.getMessage());

            } catch (RemoteException e) {
                // o servidor foi abaixo
                System.out.println("\n>> Servidor indisponivel. Volte a tentar mais tarde.");
                servico = null;
            }
        }

        System.out.println("Adeus!");
    }

    private static boolean ligar() {
        try {
            servico = (ServicoGeralRMI) Naming.lookup("//" + host + ":" + porta + "/" + nome);
            System.out.println("Ligado ao servidor (" + host + ":" + porta + ").");
            return true;
        } catch (Exception e) {
            System.out.println(">> Nao foi possivel ligar ao servidor em " + host + ":" + porta
                               + ". Verifique se o servidor esta a correr.");
            servico = null;
            return false;
        }
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("===== RESERVA DE POSTOS DE CARREGAMENTO =====");
        System.out.println(" 1. Registar utilizador");
        System.out.println(" 2. Registar posto");
        System.out.println(" 3. Reservar posto");
        System.out.println(" 4. Listar postos (com filtros)");
        System.out.println(" 5. Consultar estado de um posto");
        System.out.println(" 6. Consultar historico de um posto");
        System.out.println(" 7. Cancelar reserva");
        System.out.println(" 8. As minhas reservas");
        System.out.println(" 9. Procurar postos livres");
        System.out.println(" 0. Sair");
    }

    private static void executar(int opcao) throws ErroValidacao, RemoteException {
        switch (opcao) {
            case 1: registarUtilizador();   break;
            case 2: registarPosto();        break;
            case 3: reservar();             break;
            case 4: listarPostos();         break;
            case 5: consultarEstado();      break;
            case 6: consultarHistorico();   break;
            case 7: cancelarReserva();      break;
            case 8: minhasReservas();       break;
            case 9: procurarPostosLivres(); break;
            default: System.out.println("Opcao invalida.");
        }
    }

    private static void registarUtilizador() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Registar utilizador ---");
        String nomeUtil   = lerTexto("Nome: ");
        String email      = lerTexto("Email: ");
        String matricula  = lerTexto("Matricula: ");
        TipoFicha ficha   = lerFicha("Tipo de ficha do carro", false);

        Utilizador u = servico.registarUtilizador(nomeUtil, email, matricula, ficha);
        System.out.println("\n>> Utilizador registado: " + u);
    }

    private static void registarPosto() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Registar posto ---");
        String codigo      = lerTexto("Codigo (ex.: EVR-010): ");
        String localizacao = lerTexto("Localizacao: ");
        String cidade      = lerTexto("Cidade: ");
        double potencia    = lerDecimal("Potencia (kW): ");
        TipoFicha ficha    = lerFicha("Tipo de ficha", false);
        double preco       = lerDecimal("Preco (EUR/kWh): ");

        Posto p = servico.registarPosto(codigo, localizacao, cidade, potencia, ficha, preco);
        System.out.println("\n>> Posto registado: " + p);
        System.out.println("   (fica PENDENTE ate ser aprovado pelo administrador)");
    }

    private static void reservar() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Reservar posto ---");
        int utilizador = lerInteiro("Id do utilizador: ");
        int posto      = lerInteiro("Id do posto: ");
        String inicio  = lerTexto("Inicio (AAAA-MM-DD HH:MM): ");
        String fim     = lerTexto("Fim    (AAAA-MM-DD HH:MM): ");

        Reserva r = servico.criarReserva(utilizador, posto, inicio, fim);
        System.out.println("\n>> Reserva criada: " + r);
    }

    private static void listarPostos() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Listar postos (Enter = sem filtro) ---");
        String cidade   = lerTexto("Cidade: ");
        TipoFicha ficha = lerFicha("Tipo de ficha", true);
        String textoPot = lerTexto("Potencia minima (kW): ");
        double potencia = textoPot.isEmpty() ? 0 : converterDecimal(textoPot);

        List<Posto> postos = servico.listarPostos(cidade, ficha, potencia);
        mostrarPostos(postos);
    }

    private static void consultarEstado() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Consultar estado ---");
        int id = lerInteiro("Id do posto: ");

        Posto p = servico.consultarEstado(id);
        System.out.println("\n>> Posto " + p.getCodigo() + " (" + p.getLocalizacao() + ")");
        System.out.println("   Estado operacional:    " + p.getEstado());
        System.out.println("   Estado administrativo: " + p.getEstadoAdmin());
    }

    private static void consultarHistorico() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Consultar historico ---");
        int id = lerInteiro("Id do posto: ");

        List<EntradaHistorico> historico = servico.consultarHistorico(id);
        System.out.println();
        if (historico.isEmpty()) {
            System.out.println(">> Sem historico.");
        }
        for (EntradaHistorico h : historico) {
            System.out.println(h);
        }
    }

    private static void cancelarReserva() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Cancelar reserva ---");
        int utilizador = lerInteiro("Id do utilizador: ");
        int reserva    = lerInteiro("Id da reserva: ");

        servico.cancelarReserva(reserva, utilizador);
        System.out.println("\n>> Reserva #" + reserva + " cancelada.");
    }

    private static void minhasReservas() throws ErroValidacao, RemoteException {
        System.out.println("\n--- As minhas reservas ---");
        int utilizador = lerInteiro("Id do utilizador: ");

        List<Reserva> reservas = servico.minhasReservas(utilizador);
        System.out.println();
        if (reservas.isEmpty()) {
            System.out.println(">> Nao tem reservas.");
        }
        for (Reserva r : reservas) {
            System.out.println(r);
        }
    }

    private static void procurarPostosLivres() throws ErroValidacao, RemoteException {
        System.out.println("\n--- Procurar postos livres ---");
        int utilizador = lerInteiro("Id do utilizador: ");
        String inicio  = lerTexto("Inicio (AAAA-MM-DD HH:MM): ");
        String fim     = lerTexto("Fim    (AAAA-MM-DD HH:MM): ");

        List<Posto> postos = servico.procurarPostosLivres(utilizador, inicio, fim);
        mostrarPostos(postos);
    }

    private static void mostrarPostos(List<Posto> postos) {
        System.out.println();
        if (postos.isEmpty()) {
            System.out.println(">> Nenhum posto encontrado.");
        }
        for (Posto p : postos) {
            System.out.println(p);
        }
    }

    // leitura do teclado
    private static String lerTexto(String pergunta) {
        System.out.print(pergunta);
        return teclado.nextLine().trim();
    }

    private static int lerInteiro(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escreva um numero inteiro.");
            }
        }
    }

    private static double lerDecimal(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return converterDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escreva um numero (ex.: 50 ou 0.35).");
            }
        }
    }

    // aceita 0.35 e 0,35
    private static double converterDecimal(String texto) {
        return Double.parseDouble(texto.replace(',', '.'));
    }

    // se opcional, Enter devolve null (sem filtro)
    private static TipoFicha lerFicha(String pergunta, boolean opcional) {
        while (true) {
            String texto = lerTexto(pergunta + " (TIPO2 / CCS / CHADEMO): ").toUpperCase();
            if (opcional && texto.isEmpty()) {
                return null;
            }
            try {
                return TipoFicha.valueOf(texto);
            } catch (IllegalArgumentException e) {
                System.out.println("Tipo de ficha invalido.");
            }
        }
    }
}
