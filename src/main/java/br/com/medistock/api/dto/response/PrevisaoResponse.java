package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.PrevisaoDemanda;

import java.time.Instant;
import java.util.List;

public record PrevisaoResponse(
        String id,
        String itemId,
        Instant geradoEm,
        List<PontoSerieResponse> historico,
        List<PontoSerieResponse> previsao,
        String recomendacao,
        List<String> fatoresConsiderados
) {
    public static PrevisaoResponse de(PrevisaoDemanda demanda) {
        return new PrevisaoResponse(
                demanda.getId(),
                demanda.getItemId(),
                demanda.getGeradoEm(),
                demanda.getHistorico().stream().map(PontoSerieResponse::de).toList(),
                demanda.getPrevisao().stream().map(PontoSerieResponse::de).toList(),
                demanda.getRecomendacao(),
                demanda.getFatoresConsiderados()
        );
    }
}
