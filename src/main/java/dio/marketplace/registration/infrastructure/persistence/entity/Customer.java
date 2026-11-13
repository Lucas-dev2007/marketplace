package dio.marketplace.registration.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Representação da tabela {@code Customer}.
 *
 * <p>Fica separada de {@link dio.marketplace.registration.domain.Customer} porque
 * carrega anotações de banco, e o domínio não deve depender disso. O Spring Data
 * REST grava direto nesta classe, e é por isso que as validações moram aqui: são
 * elas que geram a resposta 400.
 */
@Entity
@Data
@RequiredArgsConstructor
public class Customer {

    @Id
    private UUID id;

    @NotBlank(message = "Informe o nome do cliente.")
    @Column(nullable = false)
    private String firstName;

    private String lastName;

    @NotBlank(message = "Informe o e-mail do cliente.")
    @Email(message = "Informe um e-mail válido, como nome@exemplo.com.")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Login do cliente na API, único. É o que liga a pessoa autenticada ao
     * cadastro dela e, por consequência, aos pedidos que ela pode ver.
     */
    @Column(length = 60, unique = true)
    private String username;

    /**
     * Hash BCrypt da senha. O {@code @JsonIgnore} é essencial: sem ele, o Spring
     * Data REST devolveria o hash em {@code GET /customers} e aceitaria senha
     * escrita direto pelo cliente da API.
     */
    @JsonIgnore
    @Column(length = 100)
    private String password;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdOn;

    /**
     * O {@code cascade} salva o endereço junto com o cliente e o
     * {@code orphanRemoval} apaga o antigo quando o cliente recebe outro.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Address address;

    // TODO: definir quem gera o id do cliente. O domínio já cria um CustomerId,
    // e este @PrePersist só cobre o caminho em que o Spring Data REST grava a
    // entidade direto, sem passar pelo domínio. Escolher um dos dois e remover o
    // outro, para não ficarem duas regras de geração convivendo.
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

}
