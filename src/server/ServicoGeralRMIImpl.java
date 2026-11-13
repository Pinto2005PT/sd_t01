package server;

import common.EntradaHistorico;
import common.ErroValidacao;
import common.Posto;
import common.Reserva;
import common.ServicoGeralRMI;
import common.TipoFicha;
import common.Utilizador;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

// Implementacao RMI do servico do cliente geral.
public class ServicoGeralRMIImpl extends UnicastRemoteObject implements ServicoGeralRMI {

    private static final long serialVersionUID = 1L;

    private final ServicoGeral servico;

    public ServicoGeralRMIImpl(ServicoGeral servico) throws RemoteException {
        super();
        this.servico = servico;
    }

    @Override
    public Utilizador registarUtilizador(String nome, String email, String matricula, TipoFicha tipoFicha)
            throws ErroValidacao {
        try {
            return servico.registarUtilizador(nome, email, matricula, tipoFicha);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public Posto registarPosto(String codigo, String localizacao, String cidade,
                               double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws ErroValidacao {
        try {
            return servico.registarPosto(codigo, localizacao, cidade, potenciaKw, tipoFicha, precoKwh);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public Reserva criarReserva(int utilizadorId, int postoId, String inicio, String fim)
            throws ErroValidacao {
        try {
            return servico.criarReserva(utilizadorId, postoId, inicio, fim);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public List<Posto> listarPostos(String cidade, TipoFicha tipoFicha, double potenciaMinima)
            throws ErroValidacao {
        try {
            return servico.listarPostos(cidade, tipoFicha, potenciaMinima);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public Posto consultarEstado(int postoId) throws ErroValidacao {
        try {
            return servico.consultarEstado(postoId);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public List<EntradaHistorico> consultarHistorico(int postoId) throws ErroValidacao {
        try {
            return servico.consultarHistorico(postoId);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public void cancelarReserva(int reservaId, int utilizadorId) throws ErroValidacao {
        try {
            servico.cancelarReserva(reservaId, utilizadorId);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public List<Reserva> minhasReservas(int utilizadorId) throws ErroValidacao {
        try {
            return servico.minhasReservas(utilizadorId);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    @Override
    public List<Posto> procurarPostosLivres(int utilizadorId, String inicio, String fim)
            throws ErroValidacao {
        try {
            return servico.procurarPostosLivres(utilizadorId, inicio, fim);
        } catch (SQLException e) {
            throw erroBD(e);
        }
    }

    // erro da BD -> mensagem para o cliente
    private ErroValidacao erroBD(SQLException e) {
        System.out.println("[Servidor] Erro na BD: " + e.getMessage());
        return new ErroValidacao("Erro na base de dados (pode estar indisponivel). Tente mais tarde.");
    }
}
