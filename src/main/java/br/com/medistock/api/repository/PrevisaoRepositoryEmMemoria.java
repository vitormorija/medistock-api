package br.com.medistock.api.repository;

import br.com.medistock.api.model.PrevisaoDemanda;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "memoria")
public class PrevisaoRepositoryEmMemoria implements PrevisaoRepository {
    private final Map<String, PrevisaoDemanda> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public PrevisaoDemanda salvar(PrevisaoDemanda previsao) {
        if (previsao.getId() == null) {
            previsao.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(previsao.getId(), previsao);
        return previsao;
    }

    @Override
    public Optional<PrevisaoDemanda> buscarMaisRecentePorItem(String itemId) {
        return armazenamento.values().stream()
                .filter(previsao -> itemId.equals(previsao.getItemId()))
                .max(Comparator.comparing(PrevisaoDemanda::getGeradoEm));
    }
}
