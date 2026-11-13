package dio.marketplace.registration.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Formatos que a API de pedidos aceita e devolve.
 *
 * <p>Ficam separados das entidades para a API não expor detalhe de banco e para
 * deixar claro o que o cliente pode mandar. As validações aqui são as que geram
 * resposta 400 antes de qualquer regra de negócio rodar.
 */
public final class PedidoDtos {

    private PedidoDtos() {
    }

    /** Corpo do {@code POST /pedidos}. */
    public record NovoPedido(@NotNull(message = "Informe o cliente do pedido.") UUID clienteId,
                             @NotEmpty(message = "O pedido precisa de pelo menos um item.")
                             @Valid List<NovoItem> itens) {
    }

    public record NovoItem(@NotNull(message = "Informe o produto do item.") UUID produtoId,
                           @NotNull(message = "Informe a quantidade do item.")
                           @Min(value = 1, message = "A quantidade precisa ser pelo menos 1.")
                           Integer quantidade) {
    }

    public record ItemDoPedido(String produto, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    public record Pedido(UUID id, String cliente, String status, BigDecimal total, Instant criadoEm,
                         List<ItemDoPedido> itens) {
    }
}
