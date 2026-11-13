package dio.marketplace.registration.infrastructure.persistence.repository;

import dio.marketplace.registration.domain.Address;
import dio.marketplace.registration.domain.Customer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testa a tradução entre domínio e tabela. O repositório é substituído por um
 * dublê, então não precisa de banco.
 */
class JpaCustomerRepositoryTests {

    private final CustomerEntityRepository entityRepository = mock(CustomerEntityRepository.class);
    private final JpaCustomerRepository repository = new JpaCustomerRepository(entityRepository);

    @Test
    void deveGravarEnderecoJuntoComCliente() {
        var endereco = new Address("Rua das Flores 10", "01000-000", "São Paulo", "SP");
        var cliente = new Customer("Ana Souza", "ana@example.com", endereco);

        repository.save(cliente);

        var captor = ArgumentCaptor.forClass(
                dio.marketplace.registration.infrastructure.persistence.entity.Customer.class);
        verify(entityRepository).save(captor.capture());

        var salvo = captor.getValue();
        assertThat(salvo.getAddress()).isNotNull();
        assertThat(salvo.getAddress().getStreet()).isEqualTo("Rua das Flores 10");
        assertThat(salvo.getAddress().getPostalCode()).isEqualTo("01000-000");
        assertThat(salvo.getAddress().getCity()).isEqualTo("São Paulo");
        assertThat(salvo.getAddress().getState()).isEqualTo("SP");
    }

    @Test
    void deveManterClienteSemEndereco() {
        var cliente = new Customer("Ana Souza", "ana@example.com");

        repository.save(cliente);

        var captor = ArgumentCaptor.forClass(
                dio.marketplace.registration.infrastructure.persistence.entity.Customer.class);
        verify(entityRepository).save(captor.capture());

        assertThat(captor.getValue().getAddress()).isNull();
    }

    @Test
    void naoDeveCriarClienteSemNome() {
        assertThatThrownBy(() -> new Customer("   ", "ana@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Informe o nome do cliente");
    }
}
