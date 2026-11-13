package dio.marketplace.shared.web;

/** A operação é válida em forma, mas não faz sentido no estado atual. Vira resposta 409. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
