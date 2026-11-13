package dio.marketplace.catalog.infrastructure.persistence.repository;

import dio.marketplace.catalog.domain.ProductCategory;
import dio.marketplace.catalog.infrastructure.persistence.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Acesso ao banco de produtos. O Spring Data REST transforma esta interface no
 * endpoint {@code /products}, com leitura e escrita.
 *
 * <p>Herda {@link PagingAndSortingRepository} para a listagem vir paginada
 * ({@code /products?page=0&size=20}) em vez de devolver o catálogo inteiro em uma
 * resposta só.
 */
@Repository
public interface ProductEntityRepository extends PagingAndSortingRepository<Product, UUID>, CrudRepository<Product, UUID> {

    /**
     * Filtro por categoria, com paginação:
     * {@code /products/search/findByCategory?category=NOTEBOOK&page=0&size=20}.
     */
    Page<Product> findByCategory(ProductCategory category, Pageable pageable);

    /**
     * Busca do catálogo por nome e faixa de preço:
     * {@code /products/search/busca?termo=notebook&minimo=2000&maximo=5000&sort=price,asc}.
     *
     * <p>Os três filtros são opcionais — quando o parâmetro não vem, chega
     * {@code null} e a condição correspondente é ignorada.
     */
    @RestResource(path = "busca", rel = "busca")
    @Query("""
            select p from Product p
            where (:termo is null or lower(p.name) like lower(concat('%', :termo, '%')))
              and (:minimo is null or p.price >= :minimo)
              and (:maximo is null or p.price <= :maximo)
            """)
    Page<Product> buscar(@Param("termo") String termo,
                         @Param("minimo") BigDecimal minimo,
                         @Param("maximo") BigDecimal maximo,
                         Pageable pageable);

    /**
     * Baixa o estoque em uma única instrução SQL, condicionada a haver saldo.
     *
     * <p>É isso que torna a operação segura com vários pedidos ao mesmo tempo: o
     * banco decide e trava a linha. Quem chama recebe {@code 1} quando conseguiu
     * e {@code 0} quando o estoque não bastava — nesse caso não há o que desfazer.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantidade where p.id = :id and p.stock >= :quantidade")
    int baixarEstoque(@Param("id") UUID id, @Param("quantidade") int quantidade);

    /** Usado no cancelamento do pedido, para devolver o que foi reservado. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :quantidade where p.id = :id")
    int devolverEstoque(@Param("id") UUID id, @Param("quantidade") int quantidade);
}
