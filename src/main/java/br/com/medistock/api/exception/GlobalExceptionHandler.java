package br.com.medistock.api.exception;

import br.com.medistock.api.dto.response.CampoComErro;
import br.com.medistock.api.dto.response.ErroResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException excecao,
                                                        HttpServletRequest requisicao) {
        List<CampoComErro> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoComErro(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Falha de validação nos campos enviados", requisicao, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao,
                                                            HttpServletRequest requisicao) {
        List<CampoComErro> campos = null;

        if (excecao.getCause() instanceof InvalidFormatException causa) {
            String campo = causa.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            campos = List.of(new CampoComErro(campo, "valor inválido: " + causa.getValue()));
        }

        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo da requisição não pôde ser lido", requisicao, campos);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException excecao,
                                                                HttpServletRequest requisicao) {
        List<CampoComErro> campos = List.of(
                new CampoComErro(excecao.getName(), "valor inválido: " + excecao.getValue()));

        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Parâmetro de consulta inválido", requisicao, campos);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException excecao,
                                                            HttpServletRequest requisicao) {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException excecao,
                                                              HttpServletRequest requisicao) {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                "Nenhum endpoint corresponde a este caminho", requisicao, null);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException excecao,
                                                             HttpServletRequest requisicao) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada",
                excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErroResponse> tratarDuplicado(RecursoDuplicadoException excecao,
                                                        HttpServletRequest requisicao) {
        return montar(HttpStatus.CONFLICT, "Conflito",
                excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(BadCredentialsException excecao,
                                                                   HttpServletRequest requisicao) {
        return montar(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "Credenciais inválidas", requisicao, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AccessDeniedException excecao,
                                                           HttpServletRequest requisicao) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para executar esta operação", requisicao, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception excecao,
                                                             HttpServletRequest requisicao) {
        log.error("Erro nao tratado em {} {}", requisicao.getMethod(), requisicao.getRequestURI(), excecao);

        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor",
                "Ocorreu um erro inesperado. Contate o suporte.", requisicao, null);
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String erro, String mensagem,
                                                HttpServletRequest requisicao, List<CampoComErro> campos) {
        return ResponseEntity.status(status).body(
                ErroResponse.de(status.value(), erro, mensagem, requisicao.getRequestURI(), campos));
    }
}
