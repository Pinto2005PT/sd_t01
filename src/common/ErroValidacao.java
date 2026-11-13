package common;

/**
 * Erro "de negocio": o pedido e invalido e foi rejeitado pelo servidor.
 * A mensagem e mostrada ao utilizador, por isso deve ser clara.
 * (E uma Exception, logo e Serializable: pode viajar por RMI e por sockets.)
 */
public class ErroValidacao extends Exception {

    private static final long serialVersionUID = 1L;

    public ErroValidacao(String mensagem) {
        super(mensagem);
    }
}
