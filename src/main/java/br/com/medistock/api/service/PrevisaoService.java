package br.com.medistock.api.service;

import br.com.medistock.api.dto.response.PrevisaoResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.PontoSerie;
import br.com.medistock.api.model.PrevisaoDemanda;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import br.com.medistock.api.repository.ItemRepository;
import br.com.medistock.api.repository.MovimentacaoRepository;
import br.com.medistock.api.repository.PrevisaoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PrevisaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int MESES_DE_HISTORICO = 6;
    private static final int MESES_PROJETADOS = 3;
    private static final int JANELA_DA_MEDIA = 3;

    private final PrevisaoRepository repository;
    private final ItemRepository itemRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public PrevisaoService(PrevisaoRepository repository,
                           ItemRepository itemRepository,
                           MovimentacaoRepository movimentacaoRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public PrevisaoResponse buscarPorItem(String itemId) {
        exigirItem(itemId);

        return repository.buscarMaisRecentePorItem(itemId)
                .map(PrevisaoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma previsão gerada para o item " + itemId
                                + ". Use POST /api/v1/previsoes/" + itemId + "/gerar"));
    }

    public PrevisaoResponse gerar(String itemId) {
        Item item = exigirItem(itemId);

        Map<YearMonth, Integer> saidasPorMes = agruparSaidasPorMes(itemId);
        List<PontoSerie> historico = montarHistorico(saidasPorMes);
        int mediaMensal = calcularMedia(historico);

        PrevisaoDemanda demanda = PrevisaoDemanda.builder()
                .itemId(itemId)
                .geradoEm(Instant.now())
                .historico(historico)
                .previsao(projetar(mediaMensal, historico.isEmpty()))
                .recomendacao(montarRecomendacao(item, historico, mediaMensal))
                .fatoresConsiderados(listarFatores(item, historico, mediaMensal))
                .build();

        return PrevisaoResponse.de(repository.salvar(demanda));
    }

    private Map<YearMonth, Integer> agruparSaidasPorMes(String itemId) {
        Map<YearMonth, Integer> porMes = new TreeMap<>();

        for (MovimentacaoEstoque movimentacao : movimentacaoRepository.listarPorItem(itemId)) {
            if (movimentacao.getTipo() != TipoMovimentacao.SAIDA || movimentacao.getDataHora() == null) {
                continue;
            }
            YearMonth mes = YearMonth.from(movimentacao.getDataHora().atZone(FUSO));
            porMes.merge(mes, movimentacao.getQuantidade(), Integer::sum);
        }
        return porMes;
    }

    private List<PontoSerie> montarHistorico(Map<YearMonth, Integer> saidasPorMes) {
        if (saidasPorMes.isEmpty()) {
            return List.of();
        }

        YearMonth atual = YearMonth.now(FUSO);
        YearMonth primeiro = ((TreeMap<YearMonth, Integer>) saidasPorMes).firstKey();
        YearMonth inicio = primeiro.isBefore(atual.minusMonths(MESES_DE_HISTORICO - 1L))
                ? atual.minusMonths(MESES_DE_HISTORICO - 1L)
                : primeiro;

        Map<YearMonth, Integer> serie = new LinkedHashMap<>();
        for (YearMonth mes = inicio; !mes.isAfter(atual); mes = mes.plusMonths(1)) {
            serie.put(mes, saidasPorMes.getOrDefault(mes, 0));
        }

        return serie.entrySet().stream()
                .map(entrada -> PontoSerie.builder()
                        .periodo(entrada.getKey().toString())
                        .valor(entrada.getValue())
                        .build())
                .toList();
    }

    private int calcularMedia(List<PontoSerie> historico) {
        if (historico.isEmpty()) {
            return 0;
        }
        List<PontoSerie> janela = historico.size() <= JANELA_DA_MEDIA
                ? historico
                : historico.subList(historico.size() - JANELA_DA_MEDIA, historico.size());

        return (int) Math.round(janela.stream().mapToInt(PontoSerie::getValor).average().orElse(0));
    }

    private List<PontoSerie> projetar(int mediaMensal, boolean semHistorico) {
        if (semHistorico) {
            return List.of();
        }

        List<PontoSerie> projecao = new ArrayList<>();
        YearMonth mes = YearMonth.now(FUSO);

        for (int i = 1; i <= MESES_PROJETADOS; i++) {
            projecao.add(PontoSerie.builder()
                    .periodo(mes.plusMonths(i).toString())
                    .valor(mediaMensal)
                    .build());
        }
        return projecao;
    }

    private String montarRecomendacao(Item item, List<PontoSerie> historico, int mediaMensal) {
        if (historico.isEmpty()) {
            return "Ainda não há saídas registradas para %s. A previsão passa a ter base assim que "
                    .formatted(item.getNome())
                    + "as movimentações de estoque forem sendo registradas.";
        }
        if (mediaMensal == 0) {
            return "Não houve consumo de %s no período analisado. Não há reposição a recomendar."
                    .formatted(item.getNome());
        }

        int atual = item.getQuantidadeAtual();
        if (atual < mediaMensal) {
            return ("Estoque atual de %d %s cobre menos de um mês da demanda média (%d por mês). "
                    + "Recomenda-se repor ao menos %d unidades para atingir %d meses de cobertura.")
                    .formatted(atual, item.getUnidadeMedida(), mediaMensal,
                            mediaMensal * MESES_PROJETADOS - atual, MESES_PROJETADOS);
        }

        int mesesDeCobertura = atual / mediaMensal;
        return "Estoque atual de %d %s cobre cerca de %d mês(es) da demanda média (%d por mês)."
                .formatted(atual, item.getUnidadeMedida(), mesesDeCobertura, mediaMensal);
    }

    private List<String> listarFatores(Item item, List<PontoSerie> historico, int mediaMensal) {
        List<String> fatores = new ArrayList<>();

        fatores.add("Movimentações de saída analisadas: %d mês(es) de histórico".formatted(historico.size()));
        if (!historico.isEmpty()) {
            fatores.add("Média móvel sobre os últimos %d mês(es): %d unidades por mês"
                    .formatted(Math.min(JANELA_DA_MEDIA, historico.size()), mediaMensal));
        }
        fatores.add("Quantidade atual em estoque: %d %s"
                .formatted(item.getQuantidadeAtual(), item.getUnidadeMedida()));
        fatores.add("Quantidade mínima definida: %d".formatted(item.getQuantidadeMinima()));

        if (item.isValidadeProxima()) {
            fatores.add("Atenção: o lote atual vence em %s".formatted(item.getDataValidade()));
        }
        return fatores;
    }

    private Item exigirItem(String itemId) {
        return itemRepository.buscarPorId(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));
    }
}
