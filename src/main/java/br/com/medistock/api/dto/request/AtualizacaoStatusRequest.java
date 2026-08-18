package br.com.medistock.api.dto.request;

import br.com.medistock.api.model.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizacaoStatusRequest(

        @NotNull(message = "é obrigatório")
        StatusPedido status
) {
}
