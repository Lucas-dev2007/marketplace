package dio.marketplace.shared.web;

/** O usuário está autenticado, mas não pode mexer neste registro. Vira resposta 403. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
