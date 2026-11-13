package dio.marketplace.registration.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representação da tabela {@code Address}, usada só pelo módulo de registro.
 *
 * <p>O {@link #toString()} não é enfeite: a projeção {@code CustomerExcerpt}
 * chama esse método para devolver o endereço como texto na listagem.
 */
@Entity
@Data
@RequiredArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String street, postalCode, city, state;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdOn;

    /** Pula campos vazios para não aparecer "null" na tela. */
    @Override
    public String toString() {
        var parts = new ArrayList<String>();
        addIfPresent(parts, street);
        addIfPresent(parts, postalCode);

        if (isBlank(city)) {
            addIfPresent(parts, state);
        } else {
            parts.add(isBlank(state) ? city.trim() : city.trim() + "/" + state.trim());
        }

        return String.join(", ", parts);
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (!isBlank(value)) {
            parts.add(value.trim());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
