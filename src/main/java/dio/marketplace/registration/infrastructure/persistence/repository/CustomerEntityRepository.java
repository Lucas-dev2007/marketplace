package dio.marketplace.registration.infrastructure.persistence.repository;


import dio.marketplace.registration.infrastructure.persistence.entity.Customer;
import dio.marketplace.registration.infrastructure.persistence.entity.projection.CustomerExcerpt;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao banco de clientes. O Spring Data REST transforma esta interface nos
 * endpoints {@code /customers} e {@code /customers/{id}} — não existe controller
 * escrito à mão.
 */
@RepositoryRestResource(excerptProjection = CustomerExcerpt.class)
public interface CustomerEntityRepository extends PagingAndSortingRepository<Customer, UUID>, CrudRepository<Customer, UUID> {

    /**
     * Usado na autenticação e para descobrir de quem é o cliente logado. Não é
     * exposto como busca pública: a autenticação precisa dele, o visitante não.
     */
    @RestResource(exported = false)
    Optional<Customer> findByUsername(String username);

    /**
     * O nome do método é a consulta: "findBy" + campo + "StartingWith" +
     * "IgnoreCase". Vira
     * {@code /customers/search/findByFirstNameStartingWithIgnoreCase?firstName=a}.
     */
    List<Customer> findByFirstNameStartingWithIgnoreCase(@Param("firstName") String firstName);

    // Exclusão direta fica desligada de propósito: apagar o cliente quebraria as
    // chaves estrangeiras dos pedidos. O direito de eliminação da LGPD é atendido
    // pela anonimização, em POST /clientes/{id}/anonimizar (ClienteService), que
    // limpa os dados pessoais e preserva o histórico de compras.
    @Override
    @RestResource(exported = false)
    void deleteById(UUID id);
}
