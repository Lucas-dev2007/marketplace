package dio.marketplace.registration.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma linha do pedido.
 *
 * <p>{@code productId} aponta para o produto, mas <b>não</b> é chave estrangeira:
 * produtos ficam no banco de catálogo e pedidos no de registro, e o MySQL não
 * aceita FK entre bancos diferentes de instâncias separadas. Por isso o nome e o
 * preço também são copiados aqui — além de resolver a integridade, isso preserva
 * o histórico da compra.
 */
@Entity
@Table(name = "OrderItem")
@Getter
@Setter
@RequiredArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** Calculado, não gravado: sempre bate com preço × quantidade. */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
