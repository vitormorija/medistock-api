package br.com.medistock.api.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    private String itemId;
    private Integer quantidade;
    private BigDecimal valorUnitario;
}
