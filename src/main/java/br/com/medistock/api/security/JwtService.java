package br.com.medistock.api.security;

import br.com.medistock.api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${medistock.jwt.secret}") String segredo,
                      @Value("${medistock.jwt.expiracao-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(usuario.getId())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    public Instant calcularExpiracao() {
        return Instant.now().plusMillis(expiracaoMs);
    }

    public String extrairIdDoUsuario(String token) {
        return lerConteudo(token).getSubject();
    }

    private Claims lerConteudo(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
