package dio.marketplace.registration.interfaces.dto;

import java.util.UUID;

/**
 * Formatos de resposta da API de cliente.
 *
 * <p>Existe para o cliente logado enxergar os próprios dados sem passar por
 * {@code /customers}, que é restrito ao administrador.
 */
public final class ClienteDtos {

    private ClienteDtos() {
    }

    /**
     * O mínimo que a tela precisa saber sobre quem está logado: id (para fechar
     * pedido no próprio nome), nome e e-mail. Endereço e senha ficam de fora.
     */
    public record ClienteLogado(UUID id, String nome, String email) {
    }
}
