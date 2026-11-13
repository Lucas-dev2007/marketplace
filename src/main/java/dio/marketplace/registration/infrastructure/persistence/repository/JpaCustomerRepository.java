package dio.marketplace.registration.infrastructure.persistence.repository;

import dio.marketplace.registration.domain.Address;
import dio.marketplace.registration.domain.Customer;
import dio.marketplace.registration.domain.CustomerId;
import dio.marketplace.registration.domain.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Adaptador entre a porta {@link CustomerRepository} e a tabela de clientes.
 *
 * <p>Toda a tradução entre domínio e tabela está em {@code toEntity} e
 * {@code toDomain}, no fim da classe.
 */
@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final CustomerEntityRepository customerEntityRepository;

    public JpaCustomerRepository(CustomerEntityRepository customerEntityRepository) {
        this.customerEntityRepository = customerEntityRepository;
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        customerEntityRepository.save(toEntity(customer));
        return customer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        var entities = customerEntityRepository.findAll(Sort.by("firstName", "lastName"));
        return StreamSupport.stream(entities.spliterator(), false)
                .map(JpaCustomerRepository::toDomain)
                .toList();
    }

    /**
     * O nome inteiro vai em {@code firstName} de propósito: assim volta igual na
     * leitura, sem precisar adivinhar onde termina o primeiro nome ("Maria da
     * Silva", por exemplo).
     */
    static dio.marketplace.registration.infrastructure.persistence.entity.Customer toEntity(Customer customer) {
        var entity = new dio.marketplace.registration.infrastructure.persistence.entity.Customer();

        entity.setId(customer.getId().id());
        entity.setFirstName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setAddress(toEntity(customer.getAddress()));

        return entity;
    }

    private static dio.marketplace.registration.infrastructure.persistence.entity.Address toEntity(Address address) {
        if (address == null) {
            return null;
        }

        var entity = new dio.marketplace.registration.infrastructure.persistence.entity.Address();
        entity.setStreet(address.street());
        entity.setPostalCode(address.postalCode());
        entity.setCity(address.city());
        entity.setState(address.state());

        return entity;
    }

    static Customer toDomain(dio.marketplace.registration.infrastructure.persistence.entity.Customer entity) {
        String fullName = Optional.ofNullable(entity.getLastName())
                .filter(lastName -> !lastName.isBlank())
                .map(lastName -> entity.getFirstName() + " " + lastName)
                .orElseGet(entity::getFirstName);

        return new Customer(new CustomerId(entity.getId()), fullName, entity.getEmail(),
                toDomain(entity.getAddress()));
    }

    private static Address toDomain(dio.marketplace.registration.infrastructure.persistence.entity.Address address) {
        if (address == null) {
            return null;
        }

        return new Address(address.getStreet(), address.getPostalCode(), address.getCity(), address.getState());
    }
}
