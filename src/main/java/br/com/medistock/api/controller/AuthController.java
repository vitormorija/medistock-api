package br.com.medistock.api.controller;

import br.com.medistock.api.dto.request.LoginRequest;
import br.com.medistock.api.dto.request.RegistroRequest;
import br.com.medistock.api.dto.response.TokenResponse;
import br.com.medistock.api.dto.response.UsuarioResponse;
import br.com.medistock.api.model.Usuario;
import br.com.medistock.api.security.JwtService;
import br.com.medistock.api.security.UsuarioAutenticado;
import br.com.medistock.api.service.UsuarioService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Tag(name = "Autenticação", description = "Cadastro de usuários, login e emissão de token JWT")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final AuthenticationManager gerenciadorDeAutenticacao;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService,
                          AuthenticationManager gerenciadorDeAutenticacao,
                          JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.gerenciadorDeAutenticacao = gerenciadorDeAutenticacao;
        this.jwtService = jwtService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado"),
            @ApiResponse(responseCode = "400", description = "Falha de validação nos campos"),
            @ApiResponse(responseCode = "409", description = "E-mail ou matrícula já cadastrados"),
            @ApiResponse(responseCode = "422", description = "E-mail fora dos domínios institucionais (regra 1)")
    })
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return usuarioService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public TokenResponse entrar(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        gerenciadorDeAutenticacao.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.senha()));

        Usuario usuario = usuarioService.buscarEntidadePorEmail(email);
        return montarResposta(usuario);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo token emitido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public TokenResponse renovar(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        Usuario usuario = usuarioService.buscarEntidadePorId(autenticado.getUsuario().getId());
        return montarResposta(usuario);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<UsuarioResponse> eu(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        Usuario usuario = usuarioService.buscarEntidadePorId(autenticado.getUsuario().getId());
        return ResponseEntity.ok(UsuarioResponse.de(usuario));
    }

    private TokenResponse montarResposta(Usuario usuario) {
        return TokenResponse.de(
                jwtService.gerarToken(usuario),
                jwtService.calcularExpiracao(),
                UsuarioResponse.de(usuario));
    }
}
