package br.com.medistock.api.service;

import br.com.medistock.api.dto.response.AlertaResponse;
import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.ItemPedido;
import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.StatusPedido;
import br.com.medistock.api.model.enums.TipoAlerta;
import br.com.medistock.api.repository.AlertaRepositoryEmMemoria;
import br.com.medistock.api.repository.PedidoRepositoryEmMemoria;
import br.com.medistock.api.repository.ItemRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AlertaService: regra 6, severidades e não duplicação")
class AlertaServiceTest {
    private ItemRepositoryEmMemoria itens;
    private PedidoRepositoryEmMemoria pedidos;
    private AlertaService service;

    @BeforeEach
    void preparar() {
        itens = new ItemRepositoryEmMemoria();
        pedidos = new PedidoRepositoryEmMemoria();
        service = new AlertaService(new AlertaRepositoryEmMemoria(), itens, pedidos);
    }

    private String salvarItem(String nome, int atual, int minimo, LocalDate validade) {
        return itens.salvar(Item.builder()
                .nome(nome).categoria("Medicamento").unidadeMedida("un")
                .quantidadeAtual(atual).quantidadeMinima(minimo).dataValidade(validade)
                .build()).getId();
    }

    private void salvarPedido(String codigo, StatusPedido status, Instant eta) {
        pedidos.salvar(Pedido.builder()
                .codigo(codigo).fornecedorId("f1").status(status)
                .dataPedido(Instant.now()).etaPrevista(eta)
                .itens(List.of(ItemPedido.builder().itemId("i1").quantidade(1).build()))
                .build());
    }

    private AlertaResponse doTipo(List<AlertaResponse> alertas, TipoAlerta tipo) {
        return alertas.stream().filter(a -> a.tipo() == tipo).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("estoque zerado gera alerta ESTOQUE_CRITICO de severidade CRITICO")
    void estoqueZeradoEhCritico() {
        salvarItem("Adrenalina", 0, 20, LocalDate.now().plusYears(1));

        AlertaResponse alerta = doTipo(service.gerar(), TipoAlerta.ESTOQUE_CRITICO);

        assertEquals(SeveridadeAlerta.CRITICO, alerta.severidade());
    }

    @Test
    @DisplayName("estoque entre 30% do mínimo e o mínimo gera severidade ATENCAO")
    void estoqueBaixoEhAtencao() {
        salvarItem("Dipirona", 20, 30, LocalDate.now().plusYears(1));

        AlertaResponse alerta = doTipo(service.gerar(), TipoAlerta.ESTOQUE_CRITICO);

        assertEquals(SeveridadeAlerta.ATENCAO, alerta.severidade());
    }

    @Test
    @DisplayName("item já vencido gera alerta VALIDADE de severidade CRITICO")
    void vencidoEhCritico() {
        salvarItem("Soro", 500, 50, LocalDate.now().minusDays(5));

        AlertaResponse alerta = doTipo(service.gerar(), TipoAlerta.VALIDADE);

        assertEquals(SeveridadeAlerta.CRITICO, alerta.severidade());
    }

    @Test
    @DisplayName("validade próxima gera alerta VALIDADE de severidade ATENCAO")
    void vencendoEhAtencao() {
        salvarItem("Soro", 500, 50, LocalDate.now().plusDays(10));

        AlertaResponse alerta = doTipo(service.gerar(), TipoAlerta.VALIDADE);

        assertEquals(SeveridadeAlerta.ATENCAO, alerta.severidade());
    }

    @Test
    @DisplayName("item crítico e vencendo gera os dois alertas, apesar do status mostrar só um")
    void geraOsDoisAlertas() {
        salvarItem("Dipirona", 2, 40, LocalDate.now().plusDays(10));

        List<AlertaResponse> gerados = service.gerar();

        assertEquals(2, gerados.size());
        assertTrue(gerados.stream().anyMatch(a -> a.tipo() == TipoAlerta.ESTOQUE_CRITICO));
        assertTrue(gerados.stream().anyMatch(a -> a.tipo() == TipoAlerta.VALIDADE));
    }

    @Test
    @DisplayName("item em situação normal não gera alerta")
    void normalNaoGera() {
        salvarItem("Luva", 900, 100, LocalDate.now().plusYears(1));

        assertTrue(service.gerar().isEmpty());
    }

    @Test
    @DisplayName("pedido com SLA excedido gera alerta ATRASO_ENTREGA")
    void pedidoAtrasadoGeraAlerta() {
        salvarPedido("#0001", StatusPedido.PENDENTE, Instant.now().minus(2, ChronoUnit.DAYS));

        AlertaResponse alerta = doTipo(service.gerar(), TipoAlerta.ATRASO_ENTREGA);

        assertEquals(SeveridadeAlerta.ATENCAO, alerta.severidade());
    }

    @Test
    @DisplayName("pedido cancelado com ETA vencida não gera alerta de atraso")
    void canceladaNaoGeraAlerta() {
        salvarPedido("#0001", StatusPedido.CANCELADO, Instant.now().minus(2, ChronoUnit.DAYS));

        assertTrue(service.gerar().isEmpty());
    }

    @Test
    @DisplayName("regra 6: a segunda varredura não duplica alerta já aberto")
    void naoDuplica() {
        salvarItem("Adrenalina", 0, 20, LocalDate.now().plusYears(1));

        assertEquals(1, service.gerar().size());
        assertTrue(service.gerar().isEmpty());
        assertTrue(service.gerar().isEmpty());
        assertEquals(1, service.listar(null, null, null).size());
    }

    @Test
    @DisplayName("depois de resolvido, o alerta volta a ser criado se a condição persistir")
    void resolvidoNaoBloqueia() {
        salvarItem("Adrenalina", 0, 20, LocalDate.now().plusYears(1));
        String id = service.gerar().getFirst().id();

        service.resolver(id);

        assertEquals(1, service.gerar().size());
        assertEquals(2, service.listar(null, null, null).size(),
                "o alerta resolvido continua registrado, preservando o histórico");
    }

    @Test
    @DisplayName("os filtros combinam tipo, severidade e situação")
    void filtros() {
        salvarItem("Adrenalina", 0, 20, LocalDate.now().plusYears(1));
        salvarItem("Soro", 500, 50, LocalDate.now().plusDays(10));
        service.gerar();

        assertEquals(1, service.listar(TipoAlerta.ESTOQUE_CRITICO, null, null).size());
        assertEquals(1, service.listar(null, SeveridadeAlerta.CRITICO, null).size());
        assertEquals(2, service.listar(null, null, StatusAlerta.ATIVO).size());
        assertTrue(service.listar(null, null, StatusAlerta.RESOLVIDO).isEmpty());
    }

    @Test
    @DisplayName("pedido não entregue com ETA vencida não gera alerta de atraso")
    void naoEntregueNaoGeraAlerta() {
        salvarPedido("#0001", StatusPedido.NAO_ENTREGUE, Instant.now().minus(2, ChronoUnit.DAYS));

        assertTrue(service.gerar().isEmpty());
    }

    @Test
    @DisplayName("alerta ignorado sai dos abertos e não é recriado pela varredura")
    void ignoradoNaoVoltaNaVarredura() {
        salvarItem("Adrenalina", 0, 20, LocalDate.now().plusYears(1));
        service.ignorar(service.gerar().getFirst().id());

        assertTrue(service.gerar().isEmpty());
        assertEquals(StatusAlerta.IGNORADO,
                service.listar(null, null, null).getFirst().status());
    }

    @Test
    @DisplayName("o titulo do alerta acompanha a severidade do estoque")
    void tituloAcompanhaSeveridade() {
        salvarItem("Luva", 20, 30, LocalDate.now().plusYears(1));
        salvarItem("Adrenalina", 2, 30, LocalDate.now().plusYears(1));
        salvarItem("Mascara", 0, 30, LocalDate.now().plusYears(1));

        List<AlertaResponse> gerados = service.gerar();

        assertTrue(gerados.stream().anyMatch(a -> a.titulo().startsWith("Estoque baixo: Luva")));
        assertTrue(gerados.stream().anyMatch(a -> a.titulo().startsWith("Estoque crítico: Adrenalina")));
        assertTrue(gerados.stream().anyMatch(a -> a.titulo().startsWith("Estoque zerado: Mascara")));
    }
}
