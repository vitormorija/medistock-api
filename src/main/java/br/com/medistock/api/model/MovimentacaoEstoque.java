package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.TipoMovimentacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoque {
    private String id;
    private String itemId;
    private TipoMovimentacao tipo;
    private Integer quantidade;
    private Integer quantidadeResultante;
    private String usuarioId;
    private String pedidoId;
    private Instant dataHora;
}
