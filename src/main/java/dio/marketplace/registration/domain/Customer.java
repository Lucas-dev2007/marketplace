package dio.marketplace.registration.domain;

import lombok.Getter;
import org.springframework.util.Assert;

/**
 * Cliente no vocabulário do negócio, sem nada de JPA.
 *
 * <p>O nome fica inteiro em {@link #name}. A divisão em nome e sobrenome só existe
 * na tabela, para facilitar busca — a conversão é feita no repositório JPA.
 */
@Getter
public class Customer {

    private final CustomerId id;
    private final String name;
    private final String email;
    private final Address address;

    public Customer(CustomerId id, String name, String email, Address address) {
        Assert.notNull(id, "O cliente precisa de um identificador.");
        Assert.hasText(name, "Informe o nome do cliente.");
        Assert.hasText(email, "Informe o e-mail do cliente.");

        this.id = id;
        this.name = name.trim();
        this.email = email.trim();
        this.address = address;
    }

    /** Para cliente que já existe, com id vindo do banco. */
    public Customer(CustomerId id, String name, String email) {
        this(id, name, email, null);
    }

    /** Para cliente novo: o id é gerado na hora. */
    public Customer(String name, String email, Address address) {
        this(new CustomerId(), name, email, address);
    }

    public Customer(String name, String email) {
        this(name, email, null);
    }
}
