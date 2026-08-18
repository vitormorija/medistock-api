package br.com.medistock.api.repository;

import br.com.medistock.api.model.MovimentacaoEstoque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "memoria")
public class MovimentacaoRepositoryEmMemoria implements MovimentacaoRepository {
    private final Map<String, MovimentacaoEstoque> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao) {
        if (movimentacao.getId() == null) {
            movimentacao.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(movimentacao.getId(), movimentacao);
        return movimentacao;
    }

    @Override
    public List<MovimentacaoEstoque> listarPorItem(String itemId) {
        return armazenamento.values().stream()
                .filter(movimentacao -> itemId.equals(movimentacao.getItemId()))
                .map(MovimentacaoEstoque.class::cast)
                .toList();
    }
}
