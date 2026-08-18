package br.com.medistock.api.repository;

import br.com.medistock.api.model.Alerta;
import br.com.medistock.api.model.enums.StatusAlerta;
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
public class AlertaRepositoryEmMemoria implements AlertaRepository {
    private final Map<String, Alerta> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong(0);

    @Override
    public Alerta salvar(Alerta alerta) {
        if (alerta.getId() == null) {
            alerta.setId(String.valueOf(sequencia.incrementAndGet()));
        }
        armazenamento.put(alerta.getId(), alerta);
        return alerta;
    }

    @Override
    public Optional<Alerta> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override
    public List<Alerta> listarTodos() {
        return new ArrayList<>(armazenamento.values());
    }

    @Override
    public List<Alerta> listarNaoResolvidos() {
        return armazenamento.values().stream()
                .filter(alerta -> alerta.getStatus() != StatusAlerta.RESOLVIDO)
                .map(Alerta.class::cast)
                .toList();
    }

    @Override
    public boolean removerPorId(String id) {
        return armazenamento.remove(id) != null;
    }
}
