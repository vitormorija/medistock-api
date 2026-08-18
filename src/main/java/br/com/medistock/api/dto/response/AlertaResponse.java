package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.Alerta;
import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;

import java.time.Instant;

public record AlertaResponse(
        String id,
        TipoAlerta tipo,
        SeveridadeAlerta severidade,
        String titulo,
        String mensagem,
        String itemId,
        String pedidoId,
        StatusAlerta status,
        String acaoTomada,
        Instant criadoEm,
        Instant resolvidoEm
) {
    public static AlertaResponse de(Alerta alerta) {
        return new AlertaResponse(
                alerta.getId(),
                alerta.getTipo(),
                alerta.getSeveridade(),
                alerta.getTitulo(),
                alerta.getMensagem(),
                alerta.getItemId(),
                alerta.getPedidoId(),
                alerta.getStatus(),
                alerta.getAcaoTomada(),
                alerta.getCriadoEm(),
                alerta.getResolvidoEm()
        );
    }
}
