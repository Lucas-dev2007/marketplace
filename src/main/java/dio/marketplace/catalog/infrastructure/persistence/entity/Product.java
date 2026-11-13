package dio.marketplace.catalog.infrastructure.persistence.entity;

import dio.marketplace.catalog.domain.ProductCategory;
import dio.marketplace.catalog.infrastructure.persistence.listener.ProductListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Um produto do catálogo (notebook, celular, fone...).
 *
 * <p>Por estar no pacote do módulo, é gerenciado pelo banco de catálogo (3308) e
 * aparece na API em {@code /products}.
 *
 * <p>O índice em {@code category} existe porque o filtro por categoria é a busca
 * mais usada do catálogo; sem ele, o banco lê a tabela inteira a cada consulta.
 */
@Entity
@Table(indexes = @Index(name = "idx_product_category", columnList = "category"))
@EntityListeners(ProductListener.class)
@Data
@RequiredArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Informe o nome do produto.")
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String brand;

    /** Guardado como texto no banco, para a leitura ser legível em consultas SQL. */
    @NotNull(message = "Informe a categoria do produto.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @NotNull(message = "Informe o preço do produto.")
    @DecimalMin(value = "0.0", message = "O preço não pode ser negativo.")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Informe a quantidade em estoque.")
    @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
    @Column(nullable = false)
    private Integer stock;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
