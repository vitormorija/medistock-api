package br.com.medistock.api.security;

import br.com.medistock.api.dto.response.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class RespostaDeErroSeguranca {
    private RespostaDeErroSeguranca() {
    }

    static void escrever(HttpServletRequest requisicao, HttpServletResponse resposta,
                         ObjectMapper conversor, HttpStatus status, String erro,
                         String mensagem) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErroResponse corpo = ErroResponse.de(
                status.value(), erro, mensagem, requisicao.getRequestURI(), null);

        conversor.writeValue(resposta.getOutputStream(), corpo);
    }
}
