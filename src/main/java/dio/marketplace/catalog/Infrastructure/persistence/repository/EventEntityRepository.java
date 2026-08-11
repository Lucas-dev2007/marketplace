package dio.marketplace.catalog.Infrastructure.persistence.repository;

import dio.marketplace.catalog.Infrastructure.persistence.entity.Event;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface EventEntityRepository extends CrudRepository<Event, UUID> {
}
