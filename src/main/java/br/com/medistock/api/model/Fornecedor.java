package br.com.medistock.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fornecedor {
    private String id;
    private String nome;
    private String cnpj;
    private String email;
    private String telefone;
    private Integer slaHoras;

    @Builder.Default
    private Integer scoreConfiabilidade = 100;

    @Builder.Default
    private Integer historicoAtrasos = 0;

    @Builder.Default
    private boolean ativo = true;
}
