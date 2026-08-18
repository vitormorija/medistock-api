package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.FornecedorRequest;
import br.com.medistock.api.dto.response.FornecedorResponse;
import br.com.medistock.api.exception.RecursoDuplicadoException;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.model.Fornecedor;
import br.com.medistock.api.repository.FornecedorRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class FornecedorService {
    private static final Comparator<Fornecedor> ORDEM_PADRAO =
            Comparator.comparing(Fornecedor::getNome, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Fornecedor::getId);

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    public List<FornecedorResponse> listar() {
        return repository.listarTodos().stream()
                .sorted(ORDEM_PADRAO)
                .map(FornecedorResponse::de)
                .toList();
    }

    public FornecedorResponse buscarPorId(String id) {
        return FornecedorResponse.de(buscarEntidade(id));
    }

    public FornecedorResponse criar(FornecedorRequest request) {
        validarCnpjDisponivel(request.cnpj(), null);

        Fornecedor fornecedor = Fornecedor.builder()
                .nome(request.nome())
                .cnpj(request.cnpj())
                .email(request.email())
                .telefone(request.telefone())
                .slaHoras(request.slaHoras())
                .scoreConfiabilidade(request.scoreConfiabilidade() == null
                        ? 100 : request.scoreConfiabilidade())
                .historicoAtrasos(0)
                .ativo(true)
                .build();

        return FornecedorResponse.de(repository.salvar(fornecedor));
    }

    public FornecedorResponse atualizar(String id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarEntidade(id);
        validarCnpjDisponivel(request.cnpj(), id);

        fornecedor.setNome(request.nome());
        fornecedor.setCnpj(request.cnpj());
        fornecedor.setEmail(request.email());
        fornecedor.setTelefone(request.telefone());
        fornecedor.setSlaHoras(request.slaHoras());
        if (request.scoreConfiabilidade() != null) {
            fornecedor.setScoreConfiabilidade(request.scoreConfiabilidade());
        }

        return FornecedorResponse.de(repository.salvar(fornecedor));
    }

    public void inativar(String id) {
        Fornecedor fornecedor = buscarEntidade(id);
        fornecedor.setAtivo(false);
        repository.salvar(fornecedor);
    }

    public Fornecedor buscarEntidade(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    private void validarCnpjDisponivel(String cnpj, String idAtual) {
        repository.buscarPorCnpj(cnpj)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException("Já existe um fornecedor com o CNPJ " + cnpj);
                });
    }
}
