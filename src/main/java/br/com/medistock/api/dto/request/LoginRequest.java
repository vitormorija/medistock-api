package br.com.medistock.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um endereço de e-mail válido")
        String email,

        @NotBlank(message = "é obrigatória")
        String senha
) {
}
