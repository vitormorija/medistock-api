package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.AtualizacaoStatusRequest;
import br.com.medistock.api.dto.request.PedidoRequest;
import br.com.medistock.api.dto.request.ItemPedidoRequest;
import br.com.medistock.api.dto.response.PedidoResponse;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Fornecedor;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.enums.StatusPedido;
import br.com.medistock.api.repository.PedidoRepositoryEmMemoria;
import br.com.medistock.api.repository.FornecedorRepositoryEmMemoria;
import br.com.medistock.api.repository.ItemRepositoryEmMemoria;
import br.com.medistock.api.repository.MovimentacaoRepositoryEmMemoria;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PedidoService: regra 4, numeração e transições de status")
class PedidoServiceTest {
    private static final String USUARIO = "usuario-de-teste";
    private static final Instant AMANHA = Instant.now().plus(1, ChronoUnit.DAYS);
    private static final Instant ONTEM = Instant.now().minus(1, ChronoUnit.DAYS);

    private ItemRepositoryEmMemoria itens;
    private FornecedorRepositoryEmMemoria fornecedores;
    private MovimentacaoRepositoryEmMemoria movimentacoes;
    private PedidoService service;

    private String fornecedorId;
    private String itemId;

    @BeforeEach
    void preparar() {
        itens = new ItemRepositoryEmMemoria();
        fornecedores = new FornecedorRepositoryEmMemoria();
        movimentacoes = new MovimentacaoRepositoryEmMemoria();
        service = new PedidoService(new PedidoRepositoryEmMemoria(), fornecedores, itens, movimentacoes);

        fornecedorId = fornecedores.salvar(Fornecedor.builder()
                .nome("Cristália").cnpj("44734671000151").email("v@c.com.br")
                .slaHoras(48).ativo(true).build()).getId();

        itemId = itens.salvar(Item.builder()
                .nome("Dipirona 500mg").categoria("Medicamento").unidadeMedida("cx")
                .quantidadeAtual(10).quantidadeMinima(50)
                .dataValidade(LocalDate.now().plusYears(1)).build()).getId();
    }

    private PedidoRequest pedido(Instant eta, int quantidade) {
        return new PedidoRequest(fornecedorId, eta, null,
                List.of(new ItemPedidoRequest(itemId, quantidade, null)));
    }

    @Test
    @DisplayName("o número é gerado pelo sistema, no formato #NNNN")
    void codigoGerado() {
        PedidoResponse pedido = service.criar(pedido(AMANHA, 100));

        assertEquals("#0001", pedido.codigo());
        assertEquals(StatusPedido.PENDENTE, pedido.status());
        assertNotNull(pedido.dataPedido());
    }

    @Test
    @DisplayName("o número incrementa a cada pedido")
    void codigoIncrementa() {
        assertEquals("#0001", service.criar(pedido(AMANHA, 10)).codigo());
        assertEquals("#0002", service.criar(pedido(AMANHA, 10)).codigo());
        assertEquals("#0003", service.criar(pedido(AMANHA, 10)).codigo());
    }

    @Test
    @DisplayName("recusa pedido para fornecedor inexistente")
    void fornecedorInexistente() {
        PedidoRequest pedido = new PedidoRequest("nao-existe", AMANHA, null,
                List.of(new ItemPedidoRequest(itemId, 10, null)));

        assertThrows(RegraDeNegocioException.class, () -> service.criar(pedido));
    }

    @Test
    @DisplayName("recusa pedido para fornecedor inativo")
    void fornecedorInativo() {
        Fornecedor fornecedor = fornecedores.buscarPorId(fornecedorId).orElseThrow();
        fornecedor.setAtivo(false);
        fornecedores.salvar(fornecedor);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.criar(pedido(AMANHA, 10)));

        assertTrue(erro.getMessage().contains("inativo"));
    }

    @Test
    @DisplayName("recusa item que aponta para item inexistente")
    void itemInexistente() {
        PedidoRequest pedido = new PedidoRequest(fornecedorId, AMANHA, null,
                List.of(new ItemPedidoRequest("nao-existe", 10, null)));

        assertThrows(RegraDeNegocioException.class, () -> service.criar(pedido));
    }

    @Test
    @DisplayName("regra 4: confirmar soma as quantidades ao estoque e conclui o pedido")
    void confirmarSomaAoEstoque() {
        String id = service.criar(pedido(AMANHA, 100)).id();

        PedidoResponse confirmada = service.confirmar(id, USUARIO);

        assertEquals(StatusPedido.ENTREGUE, confirmada.status());
        assertNotNull(confirmada.dataEntrega());
        assertEquals(110, itens.buscarPorId(itemId).orElseThrow().getQuantidadeAtual());
    }

    @Test
    @DisplayName("a confirmação registra uma movimentação de entrada ligada à pedido")
    void confirmarRegistraMovimentacao() {
        String id = service.criar(pedido(AMANHA, 100)).id();

        service.confirmar(id, USUARIO);

        var movimentacao = movimentacoes.listarPorItem(itemId).getFirst();
        assertEquals(100, movimentacao.getQuantidade());
        assertEquals(110, movimentacao.getQuantidadeResultante());
        assertEquals(id, movimentacao.getPedidoId());
        assertEquals(USUARIO, movimentacao.getUsuarioId());
    }

    @Test
    @DisplayName("confirmar duas vezes é recusado, e o estoque não dobra")
    void naoConfirmaDuasVezes() {
        String id = service.criar(pedido(AMANHA, 100)).id();
        service.confirmar(id, USUARIO);

        assertThrows(RegraDeNegocioException.class, () -> service.confirmar(id, USUARIO));
        assertEquals(110, itens.buscarPorId(itemId).orElseThrow().getQuantidadeAtual());
    }

    @Test
    @DisplayName("pedido cancelado não pode ser confirmado")
    void naoConfirmaCancelada() {
        String id = service.criar(pedido(AMANHA, 100)).id();
        service.atualizarStatus(id, new AtualizacaoStatusRequest(StatusPedido.CANCELADO));

        assertThrows(RegraDeNegocioException.class, () -> service.confirmar(id, USUARIO));
        assertEquals(10, itens.buscarPorId(itemId).orElseThrow().getQuantidadeAtual());
    }

    @Test
    @DisplayName("o PATCH de status não pode marcar ENTREGUE, para não pular a entrada no estoque")
    void patchNaoMarcaEntregue() {
        String id = service.criar(pedido(AMANHA, 100)).id();

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.atualizarStatus(id, new AtualizacaoStatusRequest(StatusPedido.ENTREGUE)));

        assertTrue(erro.getMessage().contains("confirmar"));
        assertEquals(10, itens.buscarPorId(itemId).orElseThrow().getQuantidadeAtual());
    }

    @Test
    @DisplayName("o PATCH aceita os demais status")
    void patchAceitaOsDemais() {
        String id = service.criar(pedido(AMANHA, 100)).id();

        assertEquals(StatusPedido.EM_ROTA,
                service.atualizarStatus(id, new AtualizacaoStatusRequest(StatusPedido.EM_ROTA)).status());
    }

    @Test
    @DisplayName("pedido com ETA vencida aparece como atrasada")
    void atrasadaAparece() {
        service.criar(pedido(ONTEM, 10));

        assertEquals(1, service.listarAtrasados().size());
    }

    @Test
    @DisplayName("cancelar tira o pedido da lista de atrasados")
    void canceladaSaiDasAtrasadas() {
        String id = service.criar(pedido(ONTEM, 10)).id();
        assertEquals(1, service.listarAtrasados().size());

        service.atualizarStatus(id, new AtualizacaoStatusRequest(StatusPedido.CANCELADO));

        assertTrue(service.listarAtrasados().isEmpty());
        assertFalse(service.buscarPorId(id).slaExcedido());
    }

    @Test
    @DisplayName("pedido concluído não conta como atrasado")
    void entregueNaoEstaAtrasada() {
        String id = service.criar(pedido(ONTEM, 10)).id();
        service.confirmar(id, USUARIO);

        assertTrue(service.listarAtrasados().isEmpty());
    }

    @Test
    @DisplayName("o valor total é somado dos itens, não vem na requisição")
    void valorTotalSomado() {
        PedidoRequest pedido = new PedidoRequest(fornecedorId, AMANHA, null,
                List.of(new ItemPedidoRequest(itemId, 3, new BigDecimal("12.50"))));

        assertEquals(0, new BigDecimal("37.50").compareTo(service.criar(pedido).valorTotal()));
    }

    @Test
    @DisplayName("pedido sem valor em nenhum item fica com total nulo")
    void semValorTotalNulo() {
        assertNull(service.criar(pedido(AMANHA, 10)).valorTotal());
    }

    @Test
    @DisplayName("o SLA do pedido vem do fornecedor quando não é informado")
    void slaHerdadoDoFornecedor() {
        assertEquals(48, service.criar(pedido(AMANHA, 10)).slaHoras());
    }

    @Test
    @DisplayName("o SLA informado no pedido prevalece sobre o do fornecedor")
    void slaDoPedidoPrevalece() {
        PedidoRequest pedido = new PedidoRequest(fornecedorId, AMANHA, 12,
                List.of(new ItemPedidoRequest(itemId, 1, null)));

        assertEquals(12, service.criar(pedido).slaHoras());
    }

    @Test
    @DisplayName("os quatro estados finais encerram o pedido")
    void estadosFinaisEncerram() {
        for (StatusPedido status : StatusPedido.values()) {
            Pedido pedido = Pedido.builder().status(status).build();
            boolean deveriaEncerrar = status == StatusPedido.ENTREGUE
                    || status == StatusPedido.NAO_ENTREGUE
                    || status == StatusPedido.EXTRAVIO_REEMBOLSO
                    || status == StatusPedido.CANCELADO;

            assertEquals(deveriaEncerrar, pedido.isEncerrado(), "status " + status);
        }
    }
}
