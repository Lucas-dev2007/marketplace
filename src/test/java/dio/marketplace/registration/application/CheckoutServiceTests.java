package dio.marketplace.registration.application;

import dio.marketplace.catalog.domain.ProductCategory;
import dio.marketplace.catalog.infrastructure.persistence.entity.Product;
import dio.marketplace.catalog.infrastructure.persistence.repository.ProductEntityRepository;
import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import dio.marketplace.registration.interfaces.dto.PedidoDtos;
import dio.marketplace.shared.stock.EstoqueInsuficienteException;
import dio.marketplace.shared.stock.EstoquePort;
import dio.marketplace.shared.stock.ReservaEstoque;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a coordenação entre estoque e pedido, que é a parte mais delicada do
 * sistema: mexe em dois bancos e precisa manter os dois consistentes.
 *
 * <p>Sem banco: o repositório de produtos, a porta de estoque e o serviço de
 * pedidos entram como dublês.
 */
class CheckoutServiceTests {

    private final ProductEntityRepository produtos = mock(ProductEntityRepository.class);
    private final EstoquePort estoque = mock(EstoquePort.class);
    private final PedidoService pedidos = mock(PedidoService.class);
    private final CustomerEntityRepository clientes = mock(CustomerEntityRepository.class);
    private final CheckoutService checkout = new CheckoutService(produtos, estoque, pedidos, clientes);

    private final UUID produtoId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    @Test
    void deveReservarEstoqueAntesDeGravarOPedido() {
        dadoUmProduto(10);
        when(pedidos.criar(any(), anyString(), anyList())).thenReturn(pedidoDeExemplo());

        checkout.fechar(pedidoCom(2), "cliente", true);

        InOrder ordem = inOrder(estoque, pedidos);
        ordem.verify(estoque).reservar(List.of(new ReservaEstoque(produtoId, 2)));
        ordem.verify(pedidos).criar(any(), anyString(), anyList());
    }

    @Test
    void deveDevolverEstoqueQuandoAGravacaoDoPedidoFalhar() {
        dadoUmProduto(10);
        when(pedidos.criar(any(), anyString(), anyList()))
                .thenThrow(new IllegalStateException("falha ao gravar"));

        assertThatThrownBy(() -> checkout.fechar(pedidoCom(2), "cliente", true))
                .isInstanceOf(IllegalStateException.class);

        verify(estoque).devolver(List.of(new ReservaEstoque(produtoId, 2)));
    }

    @Test
    void naoDeveGravarPedidoQuandoFaltarEstoque() {
        dadoUmProduto(1);
        doThrow(new EstoqueInsuficienteException("Estoque insuficiente."))
                .when(estoque).reservar(anyList());

        assertThatThrownBy(() -> checkout.fechar(pedidoCom(5), "cliente", true))
                .isInstanceOf(EstoqueInsuficienteException.class);

        verify(pedidos, never()).criar(any(), anyString(), anyList());
        verify(estoque, never()).devolver(anyList());
    }

    private void dadoUmProduto(int estoqueDisponivel) {
        var produto = new Product();
        produto.setId(produtoId);
        produto.setName("Galaxy Buds FE");
        produto.setCategory(ProductCategory.FONE);
        produto.setPrice(new BigDecimal("499.00"));
        produto.setStock(estoqueDisponivel);

        when(produtos.findById(produtoId)).thenReturn(Optional.of(produto));
    }

    private PedidoDtos.NovoPedido pedidoCom(int quantidade) {
        return new PedidoDtos.NovoPedido(clienteId, List.of(new PedidoDtos.NovoItem(produtoId, quantidade)));
    }

    private PedidoDtos.Pedido pedidoDeExemplo() {
        return new PedidoDtos.Pedido(UUID.randomUUID(), "Ana Souza", "CONFIRMADO",
                new BigDecimal("998.00"), null, List.of());
    }
}
