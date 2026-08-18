package br.com.medistock.api.repository;

import br.com.medistock.api.model.Fornecedor;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepository {
    Fornecedor salvar(Fornecedor fornecedor);

    Optional<Fornecedor> buscarPorId(String id);

    Optional<Fornecedor> buscarPorCnpj(String cnpj);

    List<Fornecedor> listarTodos();
}
