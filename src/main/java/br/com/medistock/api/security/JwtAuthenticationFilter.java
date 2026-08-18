package br.com.medistock.api.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String CABECALHO = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain cadeia) throws ServletException, IOException {
        String token = extrairToken(requisicao);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, requisicao);
        }

        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(String token, HttpServletRequest requisicao) {
        try {
            UserDetails usuario = userDetailsService.carregarPorId(jwtService.extrairIdDoUsuario(token));

            if (!usuario.isEnabled()) {
                return;
            }

            var autenticacao = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));

            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (JwtException | UsernameNotFoundException excecao) {
            SecurityContextHolder.clearContext();
        }
    }

    private String extrairToken(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(CABECALHO);

        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return null;
        }
        return cabecalho.substring(PREFIXO.length()).trim();
    }
}
