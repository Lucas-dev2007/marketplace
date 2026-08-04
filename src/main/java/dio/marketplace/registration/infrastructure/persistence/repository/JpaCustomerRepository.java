package dio.marketplace.registration.infrastructure.persistence.repository;

import dio.marketplace.registration.domain.Customer;
import dio.marketplace.registration.domain.CustomerId;
import dio.marketplace.registration.domain.CustumerRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

import java.util.stream.StreamSupport;


@Repository
public class JpaCustumerRepository implements CustumerRepository {

    private final CustumerEntityRepository custumerEntityRepository;

    public JpaCustumerRepository(CustumerEntityRepository custumerEntityRepository) {
        this.custumerEntityRepository = custumerEntityRepository;
    }


    @Override
    public Customer save(Customer customer) {
        var entity = mapper(customer);
        custumerEntityRepository.save(entity);
        return customer;
    }

    @Override
    public List<Customer> FindALL() {
        var iterable = custumerEntityRepository.findAll();
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(JpaCustumerRepository::mapper)
                .toList();

    }
    private static dio.marketplace.registration.infrastructure.persistence.entity.Customer mapper(Customer customer) {
        var entity = new dio.marketplace.registration.infrastructure.persistence.entity.Customer();

        entity.setId(customer.getId().id());
        entity.setFirstName(customer.getName());
        entity.setEmail(customer.getEmail());

        return entity;
    }
    private static Customer mapper(dio.marketplace.registration.infrastructure.persistence.entity.Customer entity) {
        String fullName = Optional.ofNullable(entity.getLastName())
                .map(lastName -> entity.getFirstName() + " " + lastName)
                .orElseGet(entity::getFirstName);

        return new Customer(new CustomerId(entity.getId()), fullName, entity.getEmail());
    }

}
