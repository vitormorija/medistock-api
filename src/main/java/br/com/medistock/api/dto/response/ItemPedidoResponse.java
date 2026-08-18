package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        String itemId,
        Integer quantidade,
        BigDecimal valorUnitario
) {
    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getItemId(), item.getQuantidade(), item.getValorUnitario());
    }
}
