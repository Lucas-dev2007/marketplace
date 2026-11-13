package dio.marketplace.registration.application;

import dio.marketplace.registration.domain.OrderStatus;
import dio.marketplace.registration.infrastructure.persistence.entity.Order;
import dio.marketplace.registration.infrastructure.persistence.entity.OrderItem;
import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import dio.marketplace.registration.infrastructure.persistence.repository.OrderEntityRepository;
import dio.marketplace.registration.interfaces.dto.PedidoDtos;
import dio.marketplace.shared.stock.ReservaEstoque;
import dio.marketplace.shared.web.AcessoNegadoException;
import dio.marketplace.shared.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Regras de persistência do pedido, dentro do banco de registro.
 *
 * <p>Quem coordena o estoque é o {@link CheckoutService}; aqui só se grava e se
 * consulta. Todas as operações rodam na transação padrão, que é a do banco de
 * registro.
 */
@Service
public class PedidoService {

    private final OrderEntityRepository pedidos;
    private final CustomerEntityRepository clientes;

    public PedidoService(OrderEntityRepository pedidos, CustomerEntityRepository clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    /**
     * Grava o pedido com os itens já calculados. Chamado depois que o estoque foi
     * reservado com sucesso.
     */
    @Transactional
    public PedidoDtos.Pedido criar(UUID clienteId, String usuario, List<ItemCalculado> itens) {
        var cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));

        var pedido = new Order();
        pedido.setCustomer(cliente);
        pedido.setUsuario(usuario);
        pedido.setStatus(OrderStatus.CONFIRMADO);

        var total = BigDecimal.ZERO;
        for (var item : itens) {
            var linha = new OrderItem();
            linha.setProductId(item.produtoId());
            linha.setProductName(item.nome());
            linha.setUnitPrice(item.preco());
            linha.setQuantity(item.quantidade());
            pedido.adicionarItem(linha);
            total = total.add(item.subtotal());
        }
        pedido.setTotal(total);

        // saveAndFlush é necessário para o @CreationTimestamp já estar preenchido
        // quando o pedido é convertido em resposta — sem isso criadoEm vinha nulo.
        return paraDto(pedidos.saveAndFlush(pedido));
    }

    /** Cliente vê os próprios pedidos; administrador vê todos. */
    @Transactional(readOnly = true)
    public List<PedidoDtos.Pedido> listar(String usuario, boolean administrador) {
        var encontrados = administrador
                ? pedidos.findAllByOrderByCreatedAtDesc()
                : pedidos.findByUsuarioOrderByCreatedAtDesc(usuario);

        return encontrados.stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDtos.Pedido buscar(UUID id, String usuario, boolean administrador) {
        return paraDto(buscarPermitido(id, usuario, administrador));
    }

    /** Dados que o cancelamento precisa: status atual e o que devolver ao estoque. */
    @Transactional(readOnly = true)
    public DadosParaCancelamento dadosParaCancelamento(UUID id, String usuario, boolean administrador) {
        var pedido = buscarPermitido(id, usuario, administrador);

        var reservas = pedido.getItens().stream()
                .map(item -> new ReservaEstoque(item.getProductId(), item.getQuantity()))
                .toList();

        return new DadosParaCancelamento(pedido.getId(), pedido.getStatus(), reservas);
    }

    @Transactional
    public PedidoDtos.Pedido marcarComoCancelado(UUID id) {
        var pedido = pedidos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        pedido.setStatus(OrderStatus.CANCELADO);
        return paraDto(pedidos.save(pedido));
    }

    /**
     * O pedido existe? E quem está pedindo pode ver? As duas perguntas juntas
     * evitam que a checagem de dono seja esquecida em um endpoint novo.
     */
    private Order buscarPermitido(UUID id, String usuario, boolean administrador) {
        var pedido = pedidos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        if (!administrador && !pedido.getUsuario().equals(usuario)) {
            throw new AcessoNegadoException("Este pedido pertence a outro cliente.");
        }

        return pedido;
    }

    private PedidoDtos.Pedido paraDto(Order pedido) {
        var itens = pedido.getItens().stream()
                .map(item -> new PedidoDtos.ItemDoPedido(
                        item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();

        return new PedidoDtos.Pedido(
                pedido.getId(),
                nomeDoCliente(pedido),
                pedido.getStatus().name(),
                pedido.getTotal(),
                pedido.getCreatedAt(),
                itens);
    }

    private String nomeDoCliente(Order pedido) {
        var cliente = pedido.getCustomer();
        var sobrenome = cliente.getLastName();
        return sobrenome == null || sobrenome.isBlank()
                ? cliente.getFirstName()
                : cliente.getFirstName() + " " + sobrenome;
    }

    public record DadosParaCancelamento(UUID id, OrderStatus status, List<ReservaEstoque> reservas) {
    }
}
