package dio.marketplace.registration.application;

import dio.marketplace.catalog.infrastructure.persistence.repository.ProductEntityRepository;
import dio.marketplace.registration.domain.OrderStatus;
import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import dio.marketplace.registration.interfaces.dto.PedidoDtos;
import dio.marketplace.shared.stock.EstoquePort;
import dio.marketplace.shared.stock.ReservaEstoque;
import dio.marketplace.shared.web.AcessoNegadoException;
import dio.marketplace.shared.web.RecursoNaoEncontradoException;
import dio.marketplace.shared.web.RegraDeNegocioException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fechamento e cancelamento de pedido — a única parte do sistema que mexe em dois
 * bancos na mesma operação.
 *
 * <p><b>Por que não é um {@code @Transactional} só:</b> o estoque está no banco de
 * catálogo e o pedido no de registro. O Spring não tem transação distribuída aqui
 * (seria preciso JTA), então a garantia vem de ordem + compensação:
 *
 * <ol>
 *   <li>reserva o estoque (transação do catálogo, tudo ou nada);</li>
 *   <li>grava o pedido (transação do registro);</li>
 *   <li>se o passo 2 falhar, devolve o estoque reservado antes de propagar o erro.</li>
 * </ol>
 *
 * <p>O cancelamento faz o caminho inverso. Essa compensação não é perfeita — se o
 * processo morrer entre as duas etapas, sobra estoque reservado sem pedido. Para
 * esse caso, o certo seria um evento persistido com reprocessamento; está anotado
 * como pendência no README.
 */
@Service
public class CheckoutService {

    private final ProductEntityRepository produtos;
    private final EstoquePort estoque;
    private final PedidoService pedidos;
    private final CustomerEntityRepository clientes;

    public CheckoutService(ProductEntityRepository produtos, EstoquePort estoque, PedidoService pedidos,
                           CustomerEntityRepository clientes) {
        this.produtos = produtos;
        this.estoque = estoque;
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    public PedidoDtos.Pedido fechar(PedidoDtos.NovoPedido request, String usuario, boolean administrador) {
        garantirQuePodeComprarPara(request.clienteId(), usuario, administrador);

        var itens = resolverItens(request.itens());
        var reservas = itens.stream()
                .map(item -> new ReservaEstoque(item.produtoId(), item.quantidade()))
                .toList();

        estoque.reservar(reservas);

        try {
            return pedidos.criar(request.clienteId(), usuario, itens);
        } catch (RuntimeException erro) {
            estoque.devolver(reservas);
            throw erro;
        }
    }

    public PedidoDtos.Pedido cancelar(UUID id, String usuario, boolean administrador) {
        var dados = pedidos.dadosParaCancelamento(id, usuario, administrador);

        if (dados.status() == OrderStatus.CANCELADO) {
            throw new RegraDeNegocioException("Este pedido já está cancelado.");
        }

        estoque.devolver(dados.reservas());

        try {
            return pedidos.marcarComoCancelado(id);
        } catch (RuntimeException erro) {
            estoque.reservar(dados.reservas());
            throw erro;
        }
    }

    /**
     * Administrador compra em nome de qualquer cliente; o cliente compra apenas
     * para o próprio cadastro.
     *
     * <p>Sem esta checagem, quem entrou como cliente poderia mandar o id de outra
     * pessoa no corpo e sujar o histórico dela — o dono do pedido é o usuário, mas
     * o cadastro usado na compra é escolhido no corpo da requisição.
     */
    private void garantirQuePodeComprarPara(UUID clienteId, String usuario, boolean administrador) {
        if (administrador) {
            return;
        }

        var proprio = clientes.findByUsername(usuario)
                .orElseThrow(() -> new AcessoNegadoException(
                        "Este usuário não está ligado a um cadastro de cliente."));

        if (!proprio.getId().equals(clienteId)) {
            throw new AcessoNegadoException("Você só pode fechar pedidos para o seu próprio cadastro.");
        }
    }

    /**
     * Lê o preço e o nome atuais de cada produto. Feito antes de reservar estoque
     * para poder recusar um produto inexistente sem ter mexido em nada.
     */
    private List<ItemCalculado> resolverItens(List<PedidoDtos.NovoItem> itens) {
        var calculados = new ArrayList<ItemCalculado>();

        for (var item : itens) {
            var produto = produtos.findById(item.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Produto não encontrado no catálogo: " + item.produtoId()));

            calculados.add(new ItemCalculado(produto.getId(), produto.getName(), produto.getPrice(), item.quantidade()));
        }

        return calculados;
    }
}
