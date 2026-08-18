package br.com.medistock.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 150, message = "deve ter entre 2 e 150 caracteres")
        String nome,

        @NotBlank(message = "é obrigatório")
        @Pattern(regexp = "^\\d{14}$", message = "deve conter exatamente 14 dígitos, sem pontuação")
        String cnpj,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um endereço de e-mail válido")
        String email,

        String telefone,

        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que 0")
        Integer slaHoras,

        @Min(value = 0, message = "deve estar entre 0 e 100")
        @Max(value = 100, message = "deve estar entre 0 e 100")
        Integer scoreConfiabilidade
) {
}
