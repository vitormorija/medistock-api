package br.com.medistock.api.dto.request;

import br.com.medistock.api.model.enums.TipoItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ItemAtualizacaoRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 150, message = "deve ter entre 2 e 150 caracteres")
        String nome,

        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        String descricao,

        @NotBlank(message = "é obrigatória")
        String categoria,

        @NotBlank(message = "é obrigatória")
        String unidadeMedida,

        @NotNull(message = "é obrigatória")
        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Integer quantidadeAtual,

        @NotNull(message = "é obrigatória")
        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Integer quantidadeMinima,

        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Integer quantidadeMaxima,

        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Integer quantidadeRecomendadaIa,

        String localArmazenamento,

        String fornecedorId,

        TipoItem tipo,

        String lote,

        @NotNull(message = "é obrigatória")
        LocalDate dataValidade
) {
}
