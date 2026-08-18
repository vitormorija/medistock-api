package br.com.medistock.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper conversor;

    public JwtAuthenticationEntryPoint(ObjectMapper conversor) {
        this.conversor = conversor;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
                         AuthenticationException excecao) throws IOException {
        RespostaDeErroSeguranca.escrever(requisicao, resposta, conversor,
                HttpStatus.UNAUTHORIZED, "Não autorizado",
                "Token ausente, inválido ou expirado");
    }
}
