package dio.marketplace.registration.infrastructure.event;

import dio.marketplace.registration.infrastructure.persistence.entity.Customer;
import dio.marketplace.shared.seed.CredenciaisDemo;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Reage às gravações feitas pela API de clientes.
 *
 * <p>É do Spring Data REST, não do JPA: só roda quando alguém usa
 * {@code /customers}. {@code AfterCreate} dispara só em insert; {@code AfterSave},
 * em insert e update.
 *
 * <p>O log guarda apenas o id — nome, e-mail e endereço são dados pessoais.
 *
 * <p>Também é aqui que o cliente cadastrado pela API ganha login: o
 * {@code BeforeCreate} roda antes do insert, então o que ele preenche é gravado
 * junto com o cliente.
 */
@Component
@RepositoryEventHandler
public class CustomerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomerEventHandler.class);

    private final PasswordEncoder passwordEncoder;

    public CustomerEventHandler(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Dá login ao cliente novo, seguindo a regra de demonstração: usuário é a parte
     * local do e-mail e a senha é o primeiro nome + 123.
     *
     * <p>Quando o cadastro já traz usuário e senha (por exemplo, vindo do seed),
     * nada é sobrescrito.
     *
     * <p>TODO: num sistema real, gerar um token de convite e deixar o cliente
     * definir a própria senha, em vez de derivá-la do nome.
     */
    @HandleBeforeCreate
    public void handleBeforeCreate(Customer customer) {
        if (customer.getUsername() == null || customer.getUsername().isBlank()) {
            customer.setUsername(CredenciaisDemo.usuarioDoEmail(customer.getEmail()));
        }

        if (customer.getPassword() == null || customer.getPassword().isBlank()) {
            customer.setPassword(passwordEncoder.encode(CredenciaisDemo.senhaDoNome(customer.getFirstName())));
        }
    }

    @HandleAfterCreate
    public void handleAfterCreate(Customer customer) {
        logger.info("Cliente cadastrado: id={}, login={}", customer.getId(), customer.getUsername());
    }

    @HandleAfterSave
    public void handleAfterSave(Customer customer) {
        logger.info("Cliente atualizado: id={}", customer.getId());
    }

    @HandleAfterDelete
    public void handleAfterDelete(Customer customer) {
        logger.info("Cliente removido: id={}", customer.getId());
    }
}
