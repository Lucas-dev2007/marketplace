package dio.marketplace.registration.infrastructure.persistence.entity.projection;

import dio.marketplace.registration.infrastructure.persistence.entity.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Recorte de cliente usado na listagem de {@code GET /customers}.
 *
 * <p>O Spring Data REST devolve só os campos declarados aqui — é isso que mantém
 * o e-mail fora da lista, deixando-o apenas no detalhe.
 *
 * <p>O endereço vira texto pelo SpEL abaixo; o {@code ?.} devolve {@code null} em
 * vez de erro quando o cliente ainda não tem endereço.
 */
@Projection(name = "excerpt", types = Customer.class)
public interface CustomerExcerpt {

    String getFirstName();

    String getLastName();

    @Value("#{target.address?.toString()}")
    String getAddress();
}
