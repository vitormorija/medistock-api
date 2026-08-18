package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.Usuario;
import br.com.medistock.api.model.enums.PerfilUsuario;

import java.time.Instant;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String matricula,
        String departamento,
        String cargo,
        String registroProfissional,
        String hospital,
        PerfilUsuario perfil,
        boolean ativo,
        Instant criadoEm
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getMatricula(),
                usuario.getDepartamento(),
                usuario.getCargo(),
                usuario.getRegistroProfissional(),
                usuario.getHospital(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getCriadoEm()
        );
    }
}
