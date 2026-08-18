package br.com.medistock.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoDemanda {
    private String id;
    private String itemId;
    private Instant geradoEm;

    @Builder.Default
    private List<PontoSerie> historico = new ArrayList<>();

    @Builder.Default
    private List<PontoSerie> previsao = new ArrayList<>();

    private String recomendacao;

    @Builder.Default
    private List<String> fatoresConsiderados = new ArrayList<>();
}
