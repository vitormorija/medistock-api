package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.RegistroRequest;
import br.com.medistock.api.dto.response.UsuarioResponse;
import br.com.medistock.api.exception.RecursoDuplicadoException;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Usuario;
import br.com.medistock.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder codificadorDeSenha;
    private final List<String> dominiosInstitucionais;

    public UsuarioService(UsuarioRepository repository,
                          PasswordEncoder codificadorDeSenha,
                          @Value("${medistock.dominios-institucionais}") List<String> dominiosInstitucionais) {
        this.repository = repository;
        this.codificadorDeSenha = codificadorDeSenha;
        this.dominiosInstitucionais = dominiosInstitucionais;
    }

    public UsuarioResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        validarDominioInstitucional(email);

        if (repository.existePorEmail(email)) {
            throw new RecursoDuplicadoException("Já existe um usuário com o e-mail " + email);
        }
        if (repository.existePorMatricula(request.matricula())) {
            throw new RecursoDuplicadoException("Já existe um usuário com a matrícula " + request.matricula());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(email)
                .senha(codificadorDeSenha.encode(request.senha()))
                .matricula(request.matricula())
                .departamento(request.departamento())
                .cargo(request.cargo())
                .registroProfissional(request.registroProfissional())
                .hospital(request.hospital())
                .perfil(request.perfil())
                .ativo(true)
                .criadoEm(Instant.now())
                .build();

        return UsuarioResponse.de(repository.salvar(usuario));
    }

    public Usuario buscarEntidadePorId(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    public Usuario buscarEntidadePorEmail(String email) {
        return repository.buscarPorEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + email));
    }

    private void validarDominioInstitucional(String email) {
        boolean autorizado = dominiosInstitucionais.stream()
                .anyMatch(dominio -> email.endsWith("@" + dominio.toLowerCase(Locale.ROOT))
                        || email.endsWith("." + dominio.toLowerCase(Locale.ROOT)));

        if (!autorizado) {
            throw new RegraDeNegocioException(
                    "Cadastro permitido apenas para e-mail institucional. Domínios aceitos: "
                            + String.join(", ", dominiosInstitucionais));
        }
    }
}
