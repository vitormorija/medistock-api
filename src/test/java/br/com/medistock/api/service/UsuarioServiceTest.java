package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.RegistroRequest;
import br.com.medistock.api.dto.response.UsuarioResponse;
import br.com.medistock.api.exception.RecursoDuplicadoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Usuario;
import br.com.medistock.api.model.enums.PerfilUsuario;
import br.com.medistock.api.repository.UsuarioRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UsuarioService: regra 1 e unicidade do cadastro")
class UsuarioServiceTest {
    private static final String SENHA = "senhaSegura123";

    private UsuarioRepositoryEmMemoria repositorio;
    private PasswordEncoder codificador;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryEmMemoria();
        codificador = new BCryptPasswordEncoder();
        service = new UsuarioService(repositorio, codificador,
                List.of("hc.unicamp.br", "hc.usp.br", "einstein.br"));
    }

    private RegistroRequest registro(String email, String matricula) {
        return new RegistroRequest("Vitor Morija", email, SENHA, matricula,
                "Farmácia", "Farmacêutico", null, "HC Unicamp",
                PerfilUsuario.FARMACEUTICO);
    }

    @Test
    @DisplayName("aceita e-mail de domínio institucional")
    void aceitaDominioInstitucional() {
        UsuarioResponse resposta = service.registrar(
                registro("vitor@hc.unicamp.br", "EN-2024-00123"));

        assertEquals("vitor@hc.unicamp.br", resposta.email());
        assertTrue(resposta.ativo());
    }

    @Test
    @DisplayName("regra 1: recusa e-mail de domínio não autorizado")
    void recusaDominioExterno() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.registrar(registro("fulano@gmail.com", "EN-2024-00123")));

        assertTrue(erro.getMessage().contains("institucional"));
    }

    @Test
    @DisplayName("regra 1: não basta o e-mail terminar com o domínio autorizado")
    void recusaDominioParecido() {
        assertThrows(RegraDeNegocioException.class,
                () -> service.registrar(registro("fulano@falsohc.usp.br", "EN-2024-00123")),
                "falsohc.usp.br termina com hc.usp.br mas é outro domínio");
    }

    @Test
    @DisplayName("aceita subdomínio de domínio autorizado")
    void aceitaSubdominio() {
        UsuarioResponse resposta = service.registrar(
                registro("vitor@farmacia.hc.usp.br", "EN-2024-00123"));

        assertEquals("vitor@farmacia.hc.usp.br", resposta.email());
    }

    @Test
    @DisplayName("e-mail é normalizado para minúsculas")
    void normalizaEmail() {
        UsuarioResponse resposta = service.registrar(
                registro("  Vitor.Morija@HC.USP.BR  ", "EN-2024-00123"));

        assertEquals("vitor.morija@hc.usp.br", resposta.email());
    }

    @Test
    @DisplayName("recusa e-mail já cadastrado, ignorando maiúsculas")
    void recusaEmailDuplicado() {
        service.registrar(registro("vitor@hc.usp.br", "EN-2024-00123"));

        assertThrows(RecursoDuplicadoException.class,
                () -> service.registrar(registro("VITOR@hc.usp.br", "EN-2024-00999")));
    }

    @Test
    @DisplayName("recusa matrícula já cadastrada")
    void recusaMatriculaDuplicada() {
        service.registrar(registro("vitor@hc.usp.br", "EN-2024-00123"));

        assertThrows(RecursoDuplicadoException.class,
                () -> service.registrar(registro("outro@hc.usp.br", "EN-2024-00123")));
    }

    @Test
    @DisplayName("a senha é guardada como hash BCrypt, nunca em texto")
    void senhaEhHasheada() {
        UsuarioResponse resposta = service.registrar(registro("vitor@hc.usp.br", "EN-2024-00123"));
        Usuario salvo = service.buscarEntidadePorId(resposta.id());

        assertNotEquals(SENHA, salvo.getSenha());
        assertTrue(salvo.getSenha().startsWith("$2a$"), "formato de hash BCrypt");
        assertTrue(codificador.matches(SENHA, salvo.getSenha()),
                "o hash precisa conferir com a senha original");
    }

    @Test
    @DisplayName("dois usuários com a mesma senha têm hashes diferentes")
    void hashesComSalDiferente() {
        Usuario primeiro = service.buscarEntidadePorId(
                service.registrar(registro("um@hc.usp.br", "EN-2024-00001")).id());
        Usuario segundo = service.buscarEntidadePorId(
                service.registrar(registro("dois@hc.usp.br", "EN-2024-00002")).id());

        assertNotEquals(primeiro.getSenha(), segundo.getSenha(),
                "o BCrypt gera um sal novo a cada hash");
    }
}
