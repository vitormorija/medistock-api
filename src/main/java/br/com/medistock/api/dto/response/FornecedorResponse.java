package br.com.medistock.api.dto.response;

import br.com.medistock.api.model.Fornecedor;

public record FornecedorResponse(
        String id,
        String nome,
        String cnpj,
        String email,
        String telefone,
        Integer slaHoras,
        Integer scoreConfiabilidade,
        Integer historicoAtrasos,
        boolean ativo
) {
    public static FornecedorResponse de(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getNome(),
                fornecedor.getCnpj(),
                fornecedor.getEmail(),
                fornecedor.getTelefone(),
                fornecedor.getSlaHoras(),
                fornecedor.getScoreConfiabilidade(),
                fornecedor.getHistoricoAtrasos(),
                fornecedor.isAtivo()
        );
    }
}
