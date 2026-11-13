package common;

// Erro "de negocio": o pedido e invalido e foi rejeitado pelo servidor.
public class ErroValidacao extends Exception {

    private static final long serialVersionUID = 1L;

    public ErroValidacao(String mensagem) {
        super(mensagem);
    }
}
