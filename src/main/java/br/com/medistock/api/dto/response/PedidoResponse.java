package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        String id,
        String codigo,
        String fornecedorId,
        StatusPedido status,
        Instant dataPedido,
        Instant etaPrevista,
        Instant dataEntrega,
        Instant reentregaPrevistaEm,
        Integer slaHoras,
        BigDecimal valorTotal,
        BigDecimal valorReembolso,
        String motivoAtraso,
        String motivoOcorrencia,
        List<ItemPedidoResponse> itens,
        boolean slaExcedido
) {
    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCodigo(),
                pedido.getFornecedorId(),
                pedido.getStatus(),
                pedido.getDataPedido(),
                pedido.getEtaPrevista(),
                pedido.getDataEntrega(),
                pedido.getReentregaPrevistaEm(),
                pedido.getSlaHoras(),
                pedido.getValorTotal(),
                pedido.getValorReembolso(),
                pedido.getMotivoAtraso(),
                pedido.getMotivoOcorrencia(),
                pedido.getItens().stream().map(ItemPedidoResponse::de).toList(),
                pedido.isSlaExcedido()
        );
    }
}
