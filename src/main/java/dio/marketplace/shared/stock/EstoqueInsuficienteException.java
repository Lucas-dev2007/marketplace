package dio.marketplace.shared.stock;

/**
 * Erro de negócio: o pedido pede mais do que existe em estoque.
 *
 * <p>É uma exceção em tempo de execução porque a transação do catálogo precisa
 * ser desfeita quando ela aparece. O {@code ApiExceptionHandler} a traduz em
 * resposta 409 com a mensagem para o cliente.
 */
public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
