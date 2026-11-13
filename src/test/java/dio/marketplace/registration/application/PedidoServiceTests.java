package dio.marketplace.registration.application;

import dio.marketplace.registration.domain.OrderStatus;
import dio.marketplace.registration.infrastructure.persistence.entity.Customer;
import dio.marketplace.registration.infrastructure.persistence.entity.Order;
import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import dio.marketplace.registration.infrastructure.persistence.repository.OrderEntityRepository;
import dio.marketplace.registration.interfaces.dto.PedidoDtos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o cálculo do pedido: subtotal de cada item (preço × quantidade) e total
 * como soma dos subtotais.
 *
 * <p>É a conta que o cliente vê na hora de pagar, então vale travar o
 * comportamento com números que tenham centavos: arredondamento errado e
 * quantidade trocada aparecem na hora.
 *
 * <p>Sem banco: o repositório de pedidos devolve o próprio objeto que recebeu,
 * como o JPA faria depois de gravar.
 */
class PedidoServiceTests {

    private final OrderEntityRepository pedidos = mock(OrderEntityRepository.class);
    private final CustomerEntityRepository clientes = mock(CustomerEntityRepository.class);
    private final PedidoService servico = new PedidoService(pedidos, clientes);

    @Test
    void deveCalcularSubtotalDeCadaItemETotalDoPedido() {
        var clienteId = dadoUmCliente();

        // 2 × 499,00 = 998,00 | 3 × 10,50 = 31,50 | 1 × 1,99 = 1,99
        var itens = List.of(
                item("Galaxy Buds FE", "499.00", 2),
                item("Cabo USB-C", "10.50", 3),
                item("Adaptador HDMI", "1.99", 1));

        var pedido = servico.criar(clienteId, "ana.souza", itens);

        assertThat(pedido.itens())
                .extracting(PedidoDtos.ItemDoPedido::subtotal)
                .containsExactly(new BigDecimal("998.00"), new BigDecimal("31.50"), new BigDecimal("1.99"));

        // 998,00 + 31,50 + 1,99 = 1031,49
        assertThat(pedido.total()).isEqualByComparingTo("1031.49");
        assertThat(pedido.status()).isEqualTo("CONFIRMADO");
    }

    @Test
    void deveSomarItensRepetidosDoMesmoProdutoEmLinhasSeparadas() {
        var clienteId = dadoUmCliente();

        // Dois itens do mesmo produto viram duas linhas; o total soma as duas.
        var itens = List.of(
                item("Mouse", "33.33", 3),
                item("Mouse", "33.33", 1));

        var pedido = servico.criar(clienteId, "ana.souza", itens);

        assertThat(pedido.itens()).hasSize(2);
        assertThat(pedido.itens().get(0).subtotal()).isEqualByComparingTo("99.99");
        assertThat(pedido.itens().get(1).subtotal()).isEqualByComparingTo("33.33");
        assertThat(pedido.total()).isEqualByComparingTo("133.32");
    }

    @Test
    void deveGuardarNomeEPrecoDoMomentoDaCompra() {
        var clienteId = dadoUmCliente();

        var pedido = servico.criar(clienteId, "ana.souza", List.of(item("Notebook", "4299.90", 2)));

        assertThat(pedido.itens().getFirst().precoUnitario()).isEqualByComparingTo("4299.90");
        assertThat(pedido.itens().getFirst().produto()).isEqualTo("Notebook");
        assertThat(pedido.itens().getFirst().quantidade()).isEqualTo(2);

        // O que ficou gravado na entidade é a mesma conta que foi devolvida na resposta.
        var gravado = pedidoGravado();
        assertThat(gravado.getTotal()).isEqualByComparingTo("8599.80");
        assertThat(gravado.getItens().getFirst().getSubtotal()).isEqualByComparingTo("8599.80");
    }

    @Test
    void pedidoSemItensDeveSairComTotalZero() {
        var clienteId = dadoUmCliente();

        var pedido = servico.criar(clienteId, "ana.souza", List.of());

        assertThat(pedido.total()).isEqualByComparingTo("0");
        assertThat(pedido.itens()).isEmpty();
        assertThat(pedidoGravado().getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    private UUID dadoUmCliente() {
        var cliente = new Customer();
        cliente.setId(UUID.randomUUID());
        cliente.setFirstName("Ana");
        cliente.setLastName("Souza");
        cliente.setEmail("ana.souza@exemplo.com");

        when(clientes.findById(any())).thenReturn(Optional.of(cliente));

        return cliente.getId();
    }

    /** O repositório devolve o mesmo objeto, como o JPA faria depois do flush. */
    private Order pedidoGravado() {
        var capturado = ArgumentCaptor.forClass(Order.class);
        verify(pedidos, org.mockito.Mockito.atLeastOnce()).saveAndFlush(capturado.capture());
        return capturado.getValue();
    }

    private ItemCalculado item(String nome, String preco, int quantidade) {
        return new ItemCalculado(UUID.randomUUID(), nome, new BigDecimal(preco), quantidade);
    }

    {
        // Configuração de dublê usada por todos os testes.
        when(pedidos.saveAndFlush(any(Order.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }
}
