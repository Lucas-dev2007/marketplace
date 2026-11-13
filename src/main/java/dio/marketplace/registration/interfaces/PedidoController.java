package dio.marketplace.registration.interfaces;

import dio.marketplace.registration.application.CheckoutService;
import dio.marketplace.registration.application.PedidoService;
import dio.marketplace.registration.interfaces.dto.PedidoDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * API de pedidos.
 *
 * <p>Fica fora do Spring Data REST porque pedido tem regra: reservar estoque,
 * respeitar quem é o dono e não permitir alteração direta. O {@code Principal} é
 * o usuário autenticado no HTTP Basic e vira o dono do pedido.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CheckoutService checkout;
    private final PedidoService pedidos;

    public PedidoController(CheckoutService checkout, PedidoService pedidos) {
        this.checkout = checkout;
        this.pedidos = pedidos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDtos.Pedido criar(@Valid @RequestBody PedidoDtos.NovoPedido novoPedido,
                                  Principal usuario, Authentication autenticacao) {
        return checkout.fechar(novoPedido, usuario.getName(), ehAdministrador(autenticacao));
    }

    @GetMapping
    public List<PedidoDtos.Pedido> listar(Principal usuario, Authentication autenticacao) {
        return pedidos.listar(usuario.getName(), ehAdministrador(autenticacao));
    }

    @GetMapping("/{id}")
    public PedidoDtos.Pedido buscar(@PathVariable UUID id, Principal usuario, Authentication autenticacao) {
        return pedidos.buscar(id, usuario.getName(), ehAdministrador(autenticacao));
    }

    @PostMapping("/{id}/cancelar")
    public PedidoDtos.Pedido cancelar(@PathVariable UUID id, Principal usuario, Authentication autenticacao) {
        return checkout.cancelar(id, usuario.getName(), ehAdministrador(autenticacao));
    }

    /** O perfil vem das authorities montadas na configuração de segurança. */
    private boolean ehAdministrador(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));
    }
}
