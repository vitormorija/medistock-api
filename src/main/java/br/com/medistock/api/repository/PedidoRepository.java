package br.com.medistock.api.repository;

import br.com.medistock.api.model.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository {
    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(String id);

    List<Pedido> listarTodas();

    Optional<String> buscarUltimoCodigo();
}
