package br.com.medistock.api.repository;

import br.com.medistock.api.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(String id);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);

    boolean existePorMatricula(String matricula);
}
