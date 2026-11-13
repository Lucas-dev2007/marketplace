package dio.marketplace.catalog.infrastructure.stock;

import dio.marketplace.catalog.infrastructure.persistence.repository.ProductEntityRepository;
import dio.marketplace.shared.stock.EstoqueInsuficienteException;
import dio.marketplace.shared.stock.EstoquePort;
import dio.marketplace.shared.stock.ReservaEstoque;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementação da porta de estoque usando o banco de catálogo.
 *
 * <p>Repare no {@code transactionManager = "catalogTransactionManager"}: o banco
 * padrão da aplicação é o de registro, então aqui é preciso dizer qual transação
 * vale. Sem isso, a baixa de estoque rodaria na transação errada.
 *
 * <p>A transação cobre o lote inteiro. Se o terceiro item de cinco não tiver
 * estoque, os dois primeiros voltam ao estado original — é o rollback do banco,
 * não um laço de compensação escrito à mão.
 */
@Component
public class StockAdapter implements EstoquePort {

    private final ProductEntityRepository produtos;

    public StockAdapter(ProductEntityRepository produtos) {
        this.produtos = produtos;
    }

    @Override
    @Transactional(transactionManager = "catalogTransactionManager")
    public void reservar(List<ReservaEstoque> itens) {
        for (var item : itens) {
            var produto = produtos.findById(item.produtoId())
                    .orElseThrow(() -> new EstoqueInsuficienteException(
                            "Um dos produtos do pedido não está mais no catálogo."));

            var linhasAfetadas = produtos.baixarEstoque(item.produtoId(), item.quantidade());
            if (linhasAfetadas == 0) {
                throw new EstoqueInsuficienteException(
                        "Estoque insuficiente para " + produto.getName()
                                + ". Disponível: " + produto.getStock() + ".");
            }
        }
    }

    @Override
    @Transactional(transactionManager = "catalogTransactionManager")
    public void devolver(List<ReservaEstoque> itens) {
        for (var item : itens) {
            produtos.devolverEstoque(item.produtoId(), item.quantidade());
        }
    }
}
