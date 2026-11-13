package dio.marketplace.shared.web;

/** Algo que a requisição procurou não existe. Vira resposta 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
