package dio.marketplace.registration.domain;

import java.util.List;

public interface CustumerRepository {
    Customer save(Customer customer);
    List<Customer> FindALL();
}
