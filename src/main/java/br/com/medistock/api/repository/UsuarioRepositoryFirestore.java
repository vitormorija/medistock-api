package br.com.medistock.api.repository;

import br.com.medistock.api.model.Usuario;
import br.com.medistock.api.model.enums.PerfilUsuario;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraEnum;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraTexto;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class UsuarioRepositoryFirestore implements UsuarioRepository {
    private static final String COLECAO = "usuarios";

    private final Firestore firestore;

    public UsuarioRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        DocumentReference documento = usuario.getId() == null
                ? firestore.collection(COLECAO).document()
                : firestore.collection(COLECAO).document(usuario.getId());

        usuario.setId(documento.getId());
        aguardar(documento.set(paraMapa(usuario)));

        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        DocumentSnapshot documento = aguardar(firestore.collection(COLECAO).document(id).get());

        return documento.exists() ? Optional.of(paraUsuario(documento)) : Optional.empty();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return primeiroOndeCampoEhIgual("email", normalizar(email)).map(this::paraUsuario);
    }

    @Override
    public boolean existePorEmail(String email) {
        return primeiroOndeCampoEhIgual("email", normalizar(email)).isPresent();
    }

    @Override
    public boolean existePorMatricula(String matricula) {
        return primeiroOndeCampoEhIgual("matricula", matricula).isPresent();
    }

    private Optional<QueryDocumentSnapshot> primeiroOndeCampoEhIgual(String campo, String valor) {
        List<QueryDocumentSnapshot> encontrados = aguardar(firestore.collection(COLECAO)
                .whereEqualTo(campo, valor)
                .limit(1)
                .get()).getDocuments();

        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.getFirst());
    }

    private Map<String, Object> paraMapa(Usuario usuario) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", usuario.getNome());
        dados.put("email", normalizar(usuario.getEmail()));
        dados.put("senha", usuario.getSenha());
        dados.put("matricula", usuario.getMatricula());
        dados.put("departamento", usuario.getDepartamento());
        dados.put("cargo", usuario.getCargo());
        dados.put("registroProfissional", usuario.getRegistroProfissional());
        dados.put("hospital", usuario.getHospital());
        dados.put("perfil", paraTexto(usuario.getPerfil()));
        dados.put("ativo", usuario.isAtivo());
        dados.put("criadoEm", paraTimestamp(usuario.getCriadoEm()));
        return dados;
    }

    private Usuario paraUsuario(DocumentSnapshot documento) {
        return Usuario.builder()
                .id(documento.getId())
                .nome(documento.getString("nome"))
                .email(documento.getString("email"))
                .senha(documento.getString("senha"))
                .matricula(documento.getString("matricula"))
                .departamento(documento.getString("departamento"))
                .cargo(documento.getString("cargo"))
                .registroProfissional(documento.getString("registroProfissional"))
                .hospital(documento.getString("hospital"))
                .perfil(paraEnum(PerfilUsuario.class, documento.getString("perfil")))
                .ativo(Boolean.TRUE.equals(documento.getBoolean("ativo")))
                .criadoEm(paraInstant(documento.getTimestamp("criadoEm")))
                .build();
    }

    private String normalizar(String texto) {
        return texto == null ? null : texto.trim().toLowerCase(Locale.ROOT);
    }
}
