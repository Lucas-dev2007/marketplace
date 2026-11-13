package dio.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do marketplace.
 *
 * <p>É só backend: a API REST nasce do Spring Data REST em cima dos repositórios
 * JPA, e os dados ficam em dois bancos MySQL separados — {@code registration}
 * (clientes) e {@code catalog} (produtos de tecnologia), cada um com a sua configuração em
 * {@code RegistrationConfiguration} e {@code CatalogConfiguration}.
 */
@SpringBootApplication
public class MarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }

}
