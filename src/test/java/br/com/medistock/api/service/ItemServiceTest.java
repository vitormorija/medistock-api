package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.AjusteQuantidadeRequest;
import br.com.medistock.api.dto.request.ItemCriacaoRequest;
import br.com.medistock.api.dto.response.ItemResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import br.com.medistock.api.repository.FornecedorRepositoryEmMemoria;
import br.com.medistock.api.repository.ItemRepositoryEmMemoria;
import br.com.medistock.api.repository.MovimentacaoRepositoryEmMemoria;
import br.com.medistock.api.model.enums.TipoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ItemService: ajuste de estoque e regra 2")
class ItemServiceTest {
    private static final String USUARIO = "usuario-de-teste";

    private MovimentacaoRepositoryEmMemoria movimentacoes;
    private ItemService service;

    @BeforeEach
    void preparar() {
        movimentacoes = new MovimentacaoRepositoryEmMemoria();
        service = new ItemService(new ItemRepositoryEmMemoria(), movimentacoes,
                new FornecedorRepositoryEmMemoria());
    }

    private ItemResponse criarItem(int quantidade, int minimo) {
        return service.criar(new ItemCriacaoRequest(
                "Dipirona 500mg", null, "Medicamento", "cx",
                quantidade, minimo, null, null, null, null, null,
                null, LocalDate.now().plusYears(1)));
    }

    @Test
    @DisplayName("entrada soma ao estoque")
    void entradaSoma() {
        String id = criarItem(50, 10).id();

        ItemResponse resposta = service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.ENTRADA, 30), USUARIO);

        assertEquals(80, resposta.quantidadeAtual());
    }

    @Test
    @DisplayName("saída subtrai do estoque")
    void saidaSubtrai() {
        String id = criarItem(50, 10).id();

        ItemResponse resposta = service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 20), USUARIO);

        assertEquals(30, resposta.quantidadeAtual());
    }

    @Test
    @DisplayName("regra 2: saída maior que o estoque é recusada")
    void saidaNaoDeixaNegativo() {
        String id = criarItem(10, 5).id();

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.ajustarQuantidade(
                        id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 11), USUARIO));

        assertTrue(erro.getMessage().contains("negativo"));
        assertEquals(10, service.buscarPorId(id).quantidadeAtual(),
                "o estoque não pode ter sido alterado pela tentativa recusada");
    }

    @Test
    @DisplayName("regra 2: saída exatamente igual ao estoque é permitida e zera")
    void saidaExataEhPermitida() {
        String id = criarItem(10, 5).id();

        ItemResponse resposta = service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 10), USUARIO);

        assertEquals(0, resposta.quantidadeAtual());
    }

    @Test
    @DisplayName("o status é recalculado depois do ajuste, nos dois patamares")
    void statusRecalculado() {
        String id = criarItem(100, 50).id();
        assertEquals(StatusItem.NORMAL, service.buscarPorId(id).status());

        ItemResponse abaixoDoMinimo = service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 60), USUARIO);
        assertEquals(StatusItem.ATENCAO, abaixoDoMinimo.status());

        ItemResponse noFundo = service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 30), USUARIO);
        assertEquals(StatusItem.CRITICO, noFundo.status());
    }

    @Test
    @DisplayName("cada ajuste deixa uma movimentação registrada")
    void registraMovimentacao() {
        String id = criarItem(50, 10).id();

        service.ajustarQuantidade(id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 20), USUARIO);

        List<MovimentacaoEstoque> registradas = movimentacoes.listarPorItem(id);
        assertEquals(1, registradas.size());

        MovimentacaoEstoque movimentacao = registradas.getFirst();
        assertEquals(TipoMovimentacao.SAIDA, movimentacao.getTipo());
        assertEquals(20, movimentacao.getQuantidade());
        assertEquals(30, movimentacao.getQuantidadeResultante());
        assertEquals(USUARIO, movimentacao.getUsuarioId());
    }

    @Test
    @DisplayName("a tentativa recusada não gera movimentação")
    void recusaNaoRegistra() {
        String id = criarItem(10, 5).id();

        assertThrows(RegraDeNegocioException.class, () -> service.ajustarQuantidade(
                id, new AjusteQuantidadeRequest(TipoMovimentacao.SAIDA, 999), USUARIO));

        assertTrue(movimentacoes.listarPorItem(id).isEmpty());
    }

    @Test
    @DisplayName("item inexistente resulta em recurso não encontrado")
    void inexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId("nao-existe"));
    }

    @Test
    @DisplayName("quantidade máxima menor que a mínima é recusada")
    void maximoAbaixoDoMinimo() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.criar(new ItemCriacaoRequest(
                        "Dipirona 500mg", null, "Medicamento", "cx",
                        50, 100, 40, null, null, null, null,
                        null, LocalDate.now().plusYears(1))));

        assertTrue(erro.getMessage().contains("máxima"));
    }

    @Test
    @DisplayName("fornecedor inexistente no corpo é recusado, e não vira 404")
    void fornecedorInexistente() {
        assertThrows(RegraDeNegocioException.class,
                () -> service.criar(new ItemCriacaoRequest(
                        "Dipirona 500mg", null, "Medicamento", "cx",
                        50, 10, null, null, null, "nao-existe", null,
                        null, LocalDate.now().plusYears(1))));
    }

    @Test
    @DisplayName("item sem tipo informado nasce como PRIMORDIAL")
    void tipoPadrao() {
        assertEquals(TipoItem.PRIMORDIAL, criarItem(50, 10).tipo());
    }
}
