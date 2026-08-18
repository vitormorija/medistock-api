package br.com.medistock.api.dto.request;

import br.com.medistock.api.model.enums.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AjusteQuantidadeRequest(

        @NotNull(message = "é obrigatório")
        TipoMovimentacao tipo,

        @NotNull(message = "é obrigatória")
        @Positive(message = "deve ser maior que 0")
        Integer quantidade
) {
}
