package br.com.medistock.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        List<CampoComErro> campos
) {
    public static ErroResponse de(int status, String erro, String mensagem, String path,
                                  List<CampoComErro> campos) {
        return new ErroResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status,
                erro,
                mensagem,
                path,
                campos
        );
    }
}
