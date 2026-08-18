package br.com.medistock.api.dto.response;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        Instant geradoEm,
        ResumoEstoqueResponse estoque,
        List<PedidoResponse> pedidosDoDia,
        List<AlertaResponse> alertasRecentes
) {
}
