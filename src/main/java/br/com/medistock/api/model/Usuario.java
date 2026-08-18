package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.PerfilUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private String id;
    private String nome;
    private String email;
    private String senha;
    private String matricula;
    private String departamento;
    private String cargo;
    private String registroProfissional;
    private String hospital;
    private PerfilUsuario perfil;

    @Builder.Default
    private boolean ativo = true;

    private Instant criadoEm;
}
