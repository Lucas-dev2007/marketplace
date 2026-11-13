package dio.marketplace.registration.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do pedido já resolvido contra o catálogo: o nome e o preço foram lidos do
 * produto no momento da compra e vão ser gravados no pedido.
 */
public record ItemCalculado(UUID produtoId, String nome, BigDecimal preco, int quantidade) {

    public BigDecimal subtotal() {
        return preco.multiply(BigDecimal.valueOf(quantidade));
    }
}
