package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.PontoSerie;

public record PontoSerieResponse(
        String periodo,
        Integer valor
) {
    public static PontoSerieResponse de(PontoSerie ponto) {
        return new PontoSerieResponse(ponto.getPeriodo(), ponto.getValor());
    }
}
