package dio.marketplace.catalog.infrastructure.persistence.listener;

import dio.marketplace.catalog.infrastructure.persistence.entity.Product;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra no log as gravações de {@link Product}. São callbacks do JPA, chamados
 * pelo Hibernate — não pelo código da aplicação.
 *
 * <p>Eles rodam dentro da transação, antes do commit: se a transação falhar
 * depois, o log já registrou algo que não foi gravado. Serve como rastro de
 * execução, não como prova do que está no banco.
 */
public class ProductListener {
    private static final Logger logger = LoggerFactory.getLogger(ProductListener.class);

    @PostPersist
    public void onProductCreated(Product product) {
        logger.info("Produto criado: id={}, nome={}", product.getId(), product.getName());
    }

    @PostUpdate
    public void onProductUpdated(Product product) {
        logger.info("Produto atualizado: id={}, nome={}", product.getId(), product.getName());
    }

    @PostRemove
    public void onProductDeleted(Product product) {
        logger.info("Produto removido: id={}, nome={}", product.getId(), product.getName());
    }

}
