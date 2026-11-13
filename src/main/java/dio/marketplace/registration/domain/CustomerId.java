package dio.marketplace.registration.domain;

import org.springframework.util.Assert;

import java.util.UUID;

/**
 * Identificador do cliente. É um tipo próprio, e não um {@code UUID} solto, para
 * o compilador impedir trocar o id do cliente pelo id de outra coisa.
 *
 * <p>O valor nasce no domínio, então todo cliente já existe com id antes de
 * chegar ao banco.
 */
public record CustomerId(UUID id) {

    public CustomerId {
        Assert.notNull(id, "O identificador do cliente não pode ser nulo.");
    }

    public CustomerId() {
        this(UUID.randomUUID());
    }
}
