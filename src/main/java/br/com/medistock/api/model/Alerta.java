package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {
    private String id;
    private TipoAlerta tipo;
    private SeveridadeAlerta severidade;
    private String titulo;
    private String mensagem;
    private String itemId;
    private String pedidoId;

    @Builder.Default
    private StatusAlerta status = StatusAlerta.ATIVO;

    private Instant criadoEm;
    private Instant resolvidoEm;
    private String acaoTomada;
}
