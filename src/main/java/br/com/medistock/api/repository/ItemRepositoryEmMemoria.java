package br.com.medistock.api.repository;

import br.com.medistock.api.model.Item;
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
public class ItemRepositoryEmMemoria implements ItemRepository {
    private final Map<String, Item> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public Item salvar(Item item) {
        if (item.getId() == null) {
            item.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override
    public List<Item> listarTodos() {
        return new ArrayList<>(armazenamento.values());
    }

    @Override
    public boolean removerPorId(String id) {
        return armazenamento.remove(id) != null;
    }
}
