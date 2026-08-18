package br.com.medistock.api.dto.response;

public record ResumoEstoqueResponse(
        long total,
        long criticos,
        long atencao,
        long normais,
        long vencendo
) {
}
