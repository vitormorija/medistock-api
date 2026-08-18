package br.com.medistock.api.repository;

import br.com.medistock.api.model.Usuario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "memoria")
public class UsuarioRepositoryEmMemoria implements UsuarioRepository {
    private final Map<String, Usuario> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public Usuario salvar(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(usuario.getId(), usuario);
        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return armazenamento.values().stream()
                .filter(usuario -> normalizar(usuario.getEmail()).equals(normalizar(email)))
                .findFirst();
    }

    @Override
    public boolean existePorEmail(String email) {
        return buscarPorEmail(email).isPresent();
    }

    @Override
    public boolean existePorMatricula(String matricula) {
        return armazenamento.values().stream()
                .anyMatch(usuario -> usuario.getMatricula().equalsIgnoreCase(matricula));
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
