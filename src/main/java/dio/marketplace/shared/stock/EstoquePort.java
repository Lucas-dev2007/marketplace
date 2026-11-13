package dio.marketplace.shared.stock;

import java.util.List;

/**
 * Porta de saída para mexer no estoque, que vive no banco de catálogo.
 *
 * <p>O módulo de registro depende desta interface, não do catálogo. Quem
 * implementa hoje é o {@code StockAdapter}; para testar o fechamento de pedido
 * sem banco, basta criar uma implementação de mentira.
 */
public interface EstoquePort {

    /**
     * Baixa o estoque de todos os itens de uma vez. Ou todos são reservados, ou
     * nenhum é — quem garante isso é a transação do catálogo.
     *
     * @throws EstoqueInsuficienteException se algum produto não existir ou não
     *                                     tiver quantidade suficiente.
     */
    void reservar(List<ReservaEstoque> itens);

    /** Devolve ao estoque o que havia sido reservado (usado no cancelamento). */
    void devolver(List<ReservaEstoque> itens);
}
