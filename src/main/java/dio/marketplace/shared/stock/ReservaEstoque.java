package dio.marketplace.shared.stock;

import java.util.UUID;

/**
 * Quantidade de um produto que um pedido precisa reservar.
 *
 * <p>É o "contrato de dados" entre o módulo de registro (que cria o pedido) e o
 * de catálogo (que mexe no estoque). Os dois bancos são diferentes, então essa
 * conversa acontece por código, não por chave estrangeira.
 */
public record ReservaEstoque(UUID produtoId, int quantidade) {
}
