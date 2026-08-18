package br.com.medistock.api.dto.response;

import java.util.List;

public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {
    public static <T> PaginaResponse<T> fatiar(List<T> todos, int pagina, int tamanho) {
        int totalElementos = todos.size();
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanho);
        int inicio = (int) Math.min((long) pagina * tamanho, totalElementos);
        int fim = Math.min(inicio + tamanho, totalElementos);
        return new PaginaResponse<>(
                List.copyOf(todos.subList(inicio, fim)),
                pagina,
                tamanho,
                totalElementos,
                totalPaginas
        );
    }
}
