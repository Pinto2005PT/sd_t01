package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Interface remota (RMI) usada pelo cliente geral
public interface ServicoGeralRMI extends Remote {

    Utilizador registarUtilizador(String nome, String email, String matricula, TipoFicha tipoFicha)
            throws RemoteException, ErroValidacao;

    Posto registarPosto(String codigo, String localizacao, String cidade,
                        double potenciaKw, TipoFicha tipoFicha, double precoKwh)
            throws RemoteException, ErroValidacao;

    Reserva criarReserva(int utilizadorId, int postoId, String inicio, String fim)
            throws RemoteException, ErroValidacao;

    List<Posto> listarPostos(String cidade, TipoFicha tipoFicha, double potenciaMinima)
            throws RemoteException, ErroValidacao;

    Posto consultarEstado(int postoId)
            throws RemoteException, ErroValidacao;

    List<EntradaHistorico> consultarHistorico(int postoId)
            throws RemoteException, ErroValidacao;

    // funcionalidades proprias
    void cancelarReserva(int reservaId, int utilizadorId)
            throws RemoteException, ErroValidacao;

    List<Reserva> minhasReservas(int utilizadorId)
            throws RemoteException, ErroValidacao;

    List<Posto> procurarPostosLivres(int utilizadorId, String inicio, String fim)
            throws RemoteException, ErroValidacao;
}
