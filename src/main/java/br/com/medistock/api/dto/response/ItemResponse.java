package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.model.enums.TipoItem;

import java.time.Instant;
import java.time.LocalDate;

public record ItemResponse(
        String id,
        String nome,
        String descricao,
        String categoria,
        String unidadeMedida,
        Integer quantidadeAtual,
        Integer quantidadeMinima,
        Integer quantidadeMaxima,
        Integer quantidadeRecomendadaIa,
        String localArmazenamento,
        String fornecedorId,
        TipoItem tipo,
        String lote,
        LocalDate dataValidade,
        StatusItem status,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ItemResponse de(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getNome(),
                item.getDescricao(),
                item.getCategoria(),
                item.getUnidadeMedida(),
                item.getQuantidadeAtual(),
                item.getQuantidadeMinima(),
                item.getQuantidadeMaxima(),
                item.getQuantidadeRecomendadaIa(),
                item.getLocalArmazenamento(),
                item.getFornecedorId(),
                item.getTipo(),
                item.getLote(),
                item.getDataValidade(),
                item.getStatus(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }
}
