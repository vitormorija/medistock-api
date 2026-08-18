package br.com.medistock.api.repository;

import br.com.medistock.api.model.Fornecedor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "memoria")
public class FornecedorRepositoryEmMemoria implements FornecedorRepository {
    private final Map<String, Fornecedor> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public Fornecedor salvar(Fornecedor fornecedor) {
        if (fornecedor.getId() == null) {
            fornecedor.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(fornecedor.getId(), fornecedor);
        return fornecedor;
    }

    @Override
    public Optional<Fornecedor> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override
    public Optional<Fornecedor> buscarPorCnpj(String cnpj) {
        return armazenamento.values().stream()
                .filter(fornecedor -> fornecedor.getCnpj().equals(cnpj))
                .findFirst();
    }

    @Override
    public List<Fornecedor> listarTodos() {
        return new ArrayList<>(armazenamento.values());
    }
}
