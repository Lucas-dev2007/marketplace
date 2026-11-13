package dio.marketplace.registration.interfaces;

import dio.marketplace.registration.application.ClienteService;
import dio.marketplace.registration.interfaces.dto.ClienteDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Ações de cliente que exigem regra própria e por isso não cabem no Spring Data
 * REST.
 *
 * <p>{@code /clientes/{id}/anonimizar} é restrito ao perfil ADMIN, porque mexe em
 * dados pessoais. {@code /clientes/eu} é liberado para qualquer usuário logado:
 * devolve só o cadastro de quem está pedindo, para a tela saber em nome de quem
 * está comprando.
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    /** Cadastro do próprio usuário autenticado (id, nome e e-mail). */
    @GetMapping("/eu")
    public ClienteDtos.ClienteLogado eu(Principal usuario) {
        return clientes.meuCadastro(usuario.getName());
    }

    /**
     * Atende pedido de exclusão de dados: nome, e-mail e endereço são apagados e o
     * histórico de pedidos continua existindo, sem identificar a pessoa.
     */
    @PostMapping("/{id}/anonimizar")
    public ResponseEntity<Void> anonimizar(@PathVariable UUID id) {
        clientes.anonimizar(id);
        return ResponseEntity.noContent().build();
    }
}
