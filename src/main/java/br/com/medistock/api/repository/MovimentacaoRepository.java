package br.com.medistock.api.repository;

import br.com.medistock.api.model.MovimentacaoEstoque;

import java.util.List;

public interface MovimentacaoRepository {
    MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao);

    List<MovimentacaoEstoque> listarPorItem(String itemId);
}
