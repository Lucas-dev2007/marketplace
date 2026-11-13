package dio.marketplace.registration.domain;

import java.util.List;

/**
 * Porta de saída do módulo de registro: o único acesso que o domínio tem ao
 * armazenamento. Nada aqui cita JPA — o adaptador concreto é o
 * {@link dio.marketplace.registration.infrastructure.persistence.repository.JpaCustomerRepository}.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    /** Clientes ordenados por nome. */
    List<Customer> findAll();
}
