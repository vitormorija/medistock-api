package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.model.enums.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    public static final int DIAS_PARA_VENCIMENTO = 30;

    private static final double FRACAO_DO_MINIMO_PARA_CRITICO = 0.3;

    private String id;
    private String nome;
    private String descricao;
    private String categoria;
    private String unidadeMedida;
    private Integer quantidadeAtual;
    private Integer quantidadeMinima;
    private Integer quantidadeMaxima;

    private Integer quantidadeRecomendadaIa;

    private String localArmazenamento;
    private String fornecedorId;

    @Builder.Default
    private TipoItem tipo = TipoItem.PRIMORDIAL;

    private String lote;
    private LocalDate dataValidade;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public boolean isEstoqueAbaixoDoMinimo() {
        return quantidadeAtual != null && quantidadeMinima != null
                && quantidadeAtual <= quantidadeMinima;
    }

    public boolean isEstoqueCritico() {
        return quantidadeAtual != null && quantidadeMinima != null
                && quantidadeAtual <= quantidadeMinima * FRACAO_DO_MINIMO_PARA_CRITICO;
    }

    public boolean isValidadeProxima() {
        return dataValidade != null
                && !dataValidade.isAfter(LocalDate.now().plusDays(DIAS_PARA_VENCIMENTO));
    }

    public boolean isValidadeVencida() {
        return dataValidade != null && dataValidade.isBefore(LocalDate.now());
    }

    public boolean isEstoqueZerado() {
        return quantidadeAtual != null && quantidadeAtual == 0;
    }

    public StatusItem getStatus() {
        if (isEstoqueCritico()) {
            return StatusItem.CRITICO;
        }
        if (isEstoqueAbaixoDoMinimo()) {
            return StatusItem.ATENCAO;
        }
        return StatusItem.NORMAL;
    }
}
