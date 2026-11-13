package dio.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe a aplicação inteira para garantir que as duas configurações de banco, os
 * repositórios e a segurança se montam sem erro.
 *
 * <p>Precisa dos dois MySQL do {@code docker-compose.yml} no ar; sem eles falha na
 * inicialização, e não por causa do código.
 */
@SpringBootTest
class MarketplaceApplicationTests {

    @Test
    void contextLoads() {
    }

}
