package br.com.medistock.api.dto.request;

import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertaRequest(

        @NotNull(message = "é obrigatório")
        TipoAlerta tipo,

        @NotNull(message = "é obrigatória")
        SeveridadeAlerta severidade,

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String titulo,

        @NotBlank(message = "é obrigatória")
        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        String mensagem,

        String itemId,

        String pedidoId
) {
}
