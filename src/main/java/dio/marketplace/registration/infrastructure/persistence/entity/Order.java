package dio.marketplace.registration.infrastructure.persistence.entity;

import dio.marketplace.registration.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de um cliente, com os itens e o total no momento da compra.
 *
 * <p>O nome da tabela é {@code Orders} porque {@code Order} é palavra reservada
 * do SQL (usada em {@code ORDER BY}) e daria erro na criação.
 *
 * <p>O total e o preço de cada item são gravados, não recalculados na leitura: se
 * o preço do produto mudar amanhã, o pedido antigo continua mostrando o valor que
 * o cliente pagou.
 *
 * <p>Aqui não se usa {@code @Data} de propósito — com relação bidirecional entre
 * pedido e itens, o {@code equals} gerado entraria em laço.
 */
@Entity
@Table(name = "Orders")
@Getter
@Setter
@RequiredArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Login de quem fez o pedido. É o que permite ao cliente ver só os seus. */
    @Column(nullable = false, length = 60)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Salvar o pedido salva os itens junto; remover um item o apaga da tabela. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> itens = new ArrayList<>();

    /** Mantém os dois lados da relação em sincronia ao montar o pedido. */
    public void adicionarItem(OrderItem item) {
        item.setOrder(this);
        itens.add(item);
    }
}
