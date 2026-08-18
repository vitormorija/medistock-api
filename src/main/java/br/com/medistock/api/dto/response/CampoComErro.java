package br.com.medistock.api.dto.response;

public record CampoComErro(
        String campo,
        String erro
) {
}
