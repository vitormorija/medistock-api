package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.AjusteQuantidadeRequest;
import br.com.medistock.api.dto.request.ItemAtualizacaoRequest;
import br.com.medistock.api.dto.request.ItemCriacaoRequest;
import br.com.medistock.api.dto.response.ItemResponse;
import br.com.medistock.api.dto.response.PaginaResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.model.enums.TipoItem;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import br.com.medistock.api.repository.FornecedorRepository;
import br.com.medistock.api.repository.ItemRepository;
import br.com.medistock.api.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ItemService {
    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private static final Comparator<Item> ORDEM_PADRAO =
            Comparator.comparing(Item::getNome, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Item::getId);

    private final ItemRepository repository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final FornecedorRepository fornecedorRepository;

    public ItemService(ItemRepository repository,
                         MovimentacaoRepository movimentacaoRepository,
                         FornecedorRepository fornecedorRepository) {
        this.repository = repository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    public PaginaResponse<ItemResponse> listar(StatusItem status, String categoria,
                                                 String busca, int pagina, int tamanho) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = tamanho <= 0
                ? TAMANHO_PAGINA_PADRAO
                : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);

        List<Item> encontrados = new ArrayList<>();
        for (Item item : repository.listarTodos()) {
            if (status != null && item.getStatus() != status) {
                continue;
            }
            if (categoria != null && !categoria.equalsIgnoreCase(item.getCategoria())) {
                continue;
            }
            if (busca != null && !contemIgnorandoCaixa(item.getNome(), busca)) {
                continue;
            }
            encontrados.add(item);
        }
        encontrados.sort(ORDEM_PADRAO);

        List<ItemResponse> filtrados = new ArrayList<>();
        for (Item item : encontrados) {
            filtrados.add(ItemResponse.de(item));
        }

        return PaginaResponse.fatiar(filtrados, paginaSegura, tamanhoSeguro);
    }

    public ItemResponse buscarPorId(String id) {
        return ItemResponse.de(buscarEntidade(id));
    }

    public ItemResponse criar(ItemCriacaoRequest request) {
        exigirMaximoAcimaDoMinimo(request.quantidadeMinima(), request.quantidadeMaxima());
        exigirFornecedorValido(request.fornecedorId());

        Instant agora = Instant.now();
        Item item = Item.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .categoria(request.categoria())
                .unidadeMedida(request.unidadeMedida())
                .quantidadeAtual(request.quantidadeAtual())
                .quantidadeMinima(request.quantidadeMinima())
                .quantidadeMaxima(request.quantidadeMaxima())
                .quantidadeRecomendadaIa(request.quantidadeRecomendadaIa())
                .localArmazenamento(request.localArmazenamento())
                .fornecedorId(request.fornecedorId())
                .tipo(request.tipo() == null ? TipoItem.PRIMORDIAL : request.tipo())
                .lote(request.lote())
                .dataValidade(request.dataValidade())
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();

        return ItemResponse.de(repository.salvar(item));
    }

    public ItemResponse atualizar(String id, ItemAtualizacaoRequest request) {
        exigirMaximoAcimaDoMinimo(request.quantidadeMinima(), request.quantidadeMaxima());
        exigirFornecedorValido(request.fornecedorId());

        Item item = buscarEntidade(id);

        item.setNome(request.nome());
        item.setDescricao(request.descricao());
        item.setCategoria(request.categoria());
        item.setUnidadeMedida(request.unidadeMedida());
        item.setQuantidadeAtual(request.quantidadeAtual());
        item.setQuantidadeMinima(request.quantidadeMinima());
        item.setQuantidadeMaxima(request.quantidadeMaxima());
        item.setQuantidadeRecomendadaIa(request.quantidadeRecomendadaIa());
        item.setLocalArmazenamento(request.localArmazenamento());
        item.setFornecedorId(request.fornecedorId());
        item.setTipo(request.tipo() == null ? TipoItem.PRIMORDIAL : request.tipo());
        item.setLote(request.lote());
        item.setDataValidade(request.dataValidade());
        item.setAtualizadoEm(Instant.now());

        return ItemResponse.de(repository.salvar(item));
    }

    public ItemResponse ajustarQuantidade(String id, AjusteQuantidadeRequest request,
                                           String usuarioId) {
        Item item = buscarEntidade(id);

        int novaQuantidade = request.tipo() == TipoMovimentacao.ENTRADA
                ? item.getQuantidadeAtual() + request.quantidade()
                : item.getQuantidadeAtual() - request.quantidade();

        if (novaQuantidade < 0) {
            throw new RegraDeNegocioException(
                    "A saída de " + request.quantidade() + " deixaria o estoque negativo. "
                            + "Quantidade disponível: " + item.getQuantidadeAtual());
        }

        Instant agora = Instant.now();
        item.setQuantidadeAtual(novaQuantidade);
        item.setAtualizadoEm(agora);
        Item salvo = repository.salvar(item);

        movimentacaoRepository.salvar(MovimentacaoEstoque.builder()
                .itemId(salvo.getId())
                .tipo(request.tipo())
                .quantidade(request.quantidade())
                .quantidadeResultante(novaQuantidade)
                .usuarioId(usuarioId)
                .dataHora(agora)
                .build());

        return ItemResponse.de(salvo);
    }

    public void remover(String id) {
        if (!repository.removerPorId(id)) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + id);
        }
    }

    public List<ItemResponse> listarPorStatus(StatusItem status) {
        return repository.listarTodos().stream()
                .filter(item -> item.getStatus() == status)
                .sorted(ORDEM_PADRAO)
                .map(ItemResponse::de)
                .toList();
    }

    public List<ItemResponse> listarVencendo() {
        List<Item> vencendo = new ArrayList<>();
        for (Item item : repository.listarTodos()) {
            if (item.isValidadeProxima()) {
                vencendo.add(item);
            }
        }
        vencendo.sort(ORDEM_PADRAO);

        List<ItemResponse> resposta = new ArrayList<>();
        for (Item item : vencendo) {
            resposta.add(ItemResponse.de(item));
        }
        return resposta;
    }

    private void exigirMaximoAcimaDoMinimo(Integer minimo, Integer maximo) {
        if (maximo != null && minimo != null && maximo < minimo) {
            throw new RegraDeNegocioException(
                    "A quantidade máxima não pode ser menor que a mínima.");
        }
    }

    private void exigirFornecedorValido(String fornecedorId) {
        if (fornecedorId != null && fornecedorRepository.buscarPorId(fornecedorId).isEmpty()) {
            throw new RegraDeNegocioException("Fornecedor não encontrado: " + fornecedorId);
        }
    }

    private Item buscarEntidade(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + id));
    }

    private boolean contemIgnorandoCaixa(String texto, String trecho) {
        return texto != null
                && texto.toLowerCase(Locale.ROOT).contains(trecho.toLowerCase(Locale.ROOT));
    }
}
