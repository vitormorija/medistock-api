package br.com.medistock.api.dto.response;

import java.time.Instant;

public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario
) {
    public static TokenResponse de(String token, Instant expiraEm, UsuarioResponse usuario) {
        return new TokenResponse(token, "Bearer", expiraEm, usuario);
    }
}
