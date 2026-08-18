package br.com.medistock.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ItemPedidoRequest(

        @NotBlank(message = "é obrigatório")
        String itemId,

        @NotNull(message = "é obrigatória")
        @Positive(message = "deve ser maior que 0")
        Integer quantidade,

        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        BigDecimal valorUnitario
) {
}
