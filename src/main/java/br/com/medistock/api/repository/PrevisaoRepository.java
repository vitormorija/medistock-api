package br.com.medistock.api.repository;

import br.com.medistock.api.model.PrevisaoDemanda;

import java.util.Optional;

public interface PrevisaoRepository {
    PrevisaoDemanda salvar(PrevisaoDemanda previsao);

    Optional<PrevisaoDemanda> buscarMaisRecentePorItem(String itemId);
}
