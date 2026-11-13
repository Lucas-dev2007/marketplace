package dio.marketplace.registration.domain;

import java.util.ArrayList;

/**
 * Endereço do cliente. Existe no domínio para que a porta
 * {@link CustomerRepository} não perca esse dado — antes disso o mapeamento
 * ignorava o endereço por completo.
 *
 * <p>Todos os campos são opcionais: cadastros antigos podem ter sido gravados
 * incompletos.
 */
public record Address(String street, String postalCode, String city, String state) {

    public Address {
        street = normalize(street);
        postalCode = normalize(postalCode);
        city = normalize(city);
        state = normalize(state);
    }

    /** Ex.: "Rua das Flores 10, 01000-000, São Paulo/SP". */
    public String formatted() {
        var parts = new ArrayList<String>();
        if (street != null) {
            parts.add(street);
        }
        if (postalCode != null) {
            parts.add(postalCode);
        }
        if (city != null) {
            parts.add(state == null ? city : city + "/" + state);
        } else if (state != null) {
            parts.add(state);
        }
        return String.join(", ", parts);
    }

    @Override
    public String toString() {
        return formatted();
    }

    /** Espaço em branco vira {@code null}, para o texto não sair cheio de buracos. */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
