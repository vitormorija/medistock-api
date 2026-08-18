package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    private String id;
    private String codigo;
    private String fornecedorId;
    private StatusPedido status;
    private Instant dataPedido;
    private Instant etaPrevista;
    private Instant dataEntrega;
    private Instant reentregaPrevistaEm;
    private Integer slaHoras;
    private BigDecimal valorTotal;
    private BigDecimal valorReembolso;
    private String motivoAtraso;
    private String motivoOcorrencia;

    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    private static final Set<StatusPedido> ESTADOS_FINAIS = EnumSet.of(
            StatusPedido.ENTREGUE,
            StatusPedido.NAO_ENTREGUE,
            StatusPedido.EXTRAVIO_REEMBOLSO,
            StatusPedido.CANCELADO);

    public boolean isEncerrado() {
        return ESTADOS_FINAIS.contains(status);
    }

    public boolean isSlaExcedido() {
        return !isEncerrado()
                && etaPrevista != null
                && Instant.now().isAfter(etaPrevista);
    }
}
