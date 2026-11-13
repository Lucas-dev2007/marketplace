package dio.marketplace.registration.infrastructure.persistence.repository;

import dio.marketplace.registration.infrastructure.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Acesso ao banco de pedidos.
 *
 * <p>Diferente dos repositórios de produto e cliente, este fica
 * <b>fora</b> do Spring Data REST ({@code exported = false}). Pedido tem regra de
 * negócio — reservar estoque, respeitar o dono — e não pode ser criado por um
 * {@code POST} genérico. Os endpoints ficam no {@code PedidoController}.
 */
@Repository
@RepositoryRestResource(exported = false)
public interface OrderEntityRepository extends JpaRepository<Order, UUID> {

    /** Usado quando quem consulta é um cliente: só os pedidos dele. */
    List<Order> findByUsuarioOrderByCreatedAtDesc(String usuario);

    /** Usado pelo administrador: todos os pedidos, do mais novo para o mais antigo. */
    List<Order> findAllByOrderByCreatedAtDesc();
}
