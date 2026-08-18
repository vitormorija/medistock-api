package br.com.medistock.api.service;

import br.com.medistock.api.dto.response.PrevisaoResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import br.com.medistock.api.repository.ItemRepositoryEmMemoria;
import br.com.medistock.api.repository.MovimentacaoRepositoryEmMemoria;
import br.com.medistock.api.repository.PrevisaoRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PrevisaoService: série de demanda e projeção")
class PrevisaoServiceTest {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private MovimentacaoRepositoryEmMemoria movimentacoes;
    private PrevisaoService service;
    private String itemId;

    @BeforeEach
    void preparar() {
        ItemRepositoryEmMemoria itens = new ItemRepositoryEmMemoria();
        movimentacoes = new MovimentacaoRepositoryEmMemoria();
        service = new PrevisaoService(new PrevisaoRepositoryEmMemoria(), itens, movimentacoes);

        itemId = itens.salvar(Item.builder()
                .nome("Dipirona 500mg").categoria("Medicamento").unidadeMedida("cx")
                .quantidadeAtual(500).quantidadeMinima(100)
                .dataValidade(LocalDate.now().plusYears(2)).build()).getId();
    }

    private void movimentar(TipoMovimentacao tipo, int quantidade) {
        movimentacoes.salvar(MovimentacaoEstoque.builder()
                .itemId(itemId).tipo(tipo).quantidade(quantidade)
                .quantidadeResultante(0).usuarioId("u1").dataHora(Instant.now())
                .build());
    }

    @Test
    @DisplayName("sem movimentação o histórico e a projeção nascem vazios")
    void semHistorico() {
        PrevisaoResponse previsao = service.gerar(itemId);

        assertTrue(previsao.historico().isEmpty());
        assertTrue(previsao.previsao().isEmpty());
        assertTrue(previsao.recomendacao().contains("Ainda não há saídas"));
    }

    @Test
    @DisplayName("só as saídas entram na demanda: entrada é reposição, não consumo")
    void entradaNaoEhDemanda() {
        movimentar(TipoMovimentacao.SAIDA, 40);
        movimentar(TipoMovimentacao.SAIDA, 25);
        movimentar(TipoMovimentacao.SAIDA, 45);
        movimentar(TipoMovimentacao.ENTRADA, 300);

        PrevisaoResponse previsao = service.gerar(itemId);

        assertEquals(1, previsao.historico().size());
        assertEquals(110, previsao.historico().getFirst().valor());
    }

    @Test
    @DisplayName("as saídas do mês são somadas no mesmo período")
    void agregaPorMes() {
        movimentar(TipoMovimentacao.SAIDA, 10);
        movimentar(TipoMovimentacao.SAIDA, 20);

        PrevisaoResponse previsao = service.gerar(itemId);

        assertEquals(YearMonth.now(FUSO).toString(), previsao.historico().getFirst().periodo());
        assertEquals(30, previsao.historico().getFirst().valor());
    }

    @Test
    @DisplayName("projeta três meses à frente usando a média móvel")
    void projetaTresMeses() {
        movimentar(TipoMovimentacao.SAIDA, 110);

        PrevisaoResponse previsao = service.gerar(itemId);

        assertEquals(3, previsao.previsao().size());
        assertTrue(previsao.previsao().stream().allMatch(p -> p.valor() == 110));
        assertEquals(YearMonth.now(FUSO).plusMonths(1).toString(),
                previsao.previsao().getFirst().periodo());
    }

    @Test
    @DisplayName("recomenda reposição quando o estoque cobre menos de um mês")
    void recomendaReposicao() {
        movimentar(TipoMovimentacao.SAIDA, 600);

        PrevisaoResponse previsao = service.gerar(itemId);

        assertTrue(previsao.recomendacao().contains("Recomenda-se repor"));
    }

    @Test
    @DisplayName("informa a cobertura quando o estoque é suficiente")
    void informaCobertura() {
        movimentar(TipoMovimentacao.SAIDA, 100);

        PrevisaoResponse previsao = service.gerar(itemId);

        assertTrue(previsao.recomendacao().contains("cobre cerca de"));
    }

    @Test
    @DisplayName("a busca devolve a previsão mais recente")
    void buscaAMaisRecente() {
        movimentar(TipoMovimentacao.SAIDA, 50);
        service.gerar(itemId);
        movimentar(TipoMovimentacao.SAIDA, 70);
        PrevisaoResponse segunda = service.gerar(itemId);

        assertEquals(segunda.id(), service.buscarPorItem(itemId).id());
        assertEquals(120, service.buscarPorItem(itemId).historico().getFirst().valor());
    }

    @Test
    @DisplayName("buscar antes de gerar resulta em recurso não encontrado")
    void buscaAntesDeGerar() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorItem(itemId));
    }

    @Test
    @DisplayName("item inexistente resulta em recurso não encontrado")
    void itemInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.gerar("nao-existe"));
    }
}
