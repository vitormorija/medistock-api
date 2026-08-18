package br.com.medistock.api.repository;

import br.com.medistock.api.model.Pedido;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "memoria")
public class PedidoRepositoryEmMemoria implements PedidoRepository {
    private final Map<String, Pedido> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public Pedido salvar(Pedido pedido) {
        if (pedido.getId() == null) {
            pedido.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(pedido.getId(), pedido);
        return pedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override
    public List<Pedido> listarTodas() {
        return new ArrayList<>(armazenamento.values());
    }

    @Override
    public Optional<String> buscarUltimoCodigo() {
        return armazenamento.values().stream()
                .map(Pedido::getCodigo)
                .max(Comparator.naturalOrder());
    }
}
