package dio.marketplace.registration.domain;

/**
 * Situação do pedido.
 *
 * <p>Só existem dois estados porque o pedido nasce confirmado: o estoque é
 * reservado na mesma chamada que o cria. Se a reserva falhar, nenhum pedido é
 * gravado. Um estado "aguardando pagamento" só faria sentido quando existir
 * cobrança de verdade.
 */
public enum OrderStatus {
    CONFIRMADO,
    CANCELADO
}
