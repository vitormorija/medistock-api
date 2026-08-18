package br.com.medistock.api.dto.request;

import br.com.medistock.api.model.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 3, max = 120, message = "deve ter entre 3 e 120 caracteres")
        String nome,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um endereço de e-mail válido")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, message = "deve ter no mínimo 8 caracteres")
        String senha,

        @NotBlank(message = "é obrigatória")
        @Pattern(regexp = "^[A-Z]{2}-\\d{4}-\\d{5}$",
                message = "deve seguir o padrão XX-AAAA-NNNNN, como EN-2024-00123")
        String matricula,

        @NotBlank(message = "é obrigatório")
        String departamento,

        @NotBlank(message = "é obrigatório")
        String cargo,

        String registroProfissional,

        String hospital,

        @NotNull(message = "é obrigatório")
        PerfilUsuario perfil
) {
}
