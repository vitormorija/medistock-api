package br.com.medistock.api.repository;

import br.com.medistock.api.model.Alerta;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository {
    Alerta salvar(Alerta alerta);

    Optional<Alerta> buscarPorId(String id);

    List<Alerta> listarTodos();

    List<Alerta> listarNaoResolvidos();

    boolean removerPorId(String id);
}
