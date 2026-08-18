package br.com.medistock.api.repository;

import br.com.medistock.api.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Item salvar(Item item);

    Optional<Item> buscarPorId(String id);

    List<Item> listarTodos();

    boolean removerPorId(String id);
}
