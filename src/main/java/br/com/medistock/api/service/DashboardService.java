package br.com.medistock.api.service;

import br.com.medistock.api.dto.response.AlertaResponse;
import br.com.medistock.api.dto.response.DashboardResponse;
import br.com.medistock.api.dto.response.PedidoResponse;
import br.com.medistock.api.dto.response.ResumoEstoqueResponse;
import br.com.medistock.api.model.Alerta;
import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.repository.AlertaRepository;
import br.com.medistock.api.repository.PedidoRepository;
import br.com.medistock.api.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int ALERTAS_NO_RESUMO = 5;

    private final ItemRepository itemRepository;
    private final PedidoRepository pedidoRepository;
    private final AlertaRepository alertaRepository;

    public DashboardService(ItemRepository itemRepository,
                            PedidoRepository pedidoRepository,
                            AlertaRepository alertaRepository) {
        this.itemRepository = itemRepository;
        this.pedidoRepository = pedidoRepository;
        this.alertaRepository = alertaRepository;
    }

    public DashboardResponse montarResumo() {
        return new DashboardResponse(
                Instant.now(),
                resumirEstoque(),
                pedidosPrevistosParaHoje(),
                alertasRecentes());
    }

    private ResumoEstoqueResponse resumirEstoque() {
        List<Item> itens = itemRepository.listarTodos();

        return new ResumoEstoqueResponse(
                itens.size(),
                contarPorStatus(itens, StatusItem.CRITICO),
                contarPorStatus(itens, StatusItem.ATENCAO),
                contarPorStatus(itens, StatusItem.NORMAL),
                contarVencendo(itens));
    }

    private long contarPorStatus(List<Item> itens, StatusItem status) {
        long total = 0;
        for (Item item : itens) {
            if (item.getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    private long contarVencendo(List<Item> itens) {
        long total = 0;
        for (Item item : itens) {
            if (item.isValidadeProxima()) {
                total++;
            }
        }
        return total;
    }

    private List<PedidoResponse> pedidosPrevistosParaHoje() {
        LocalDate hoje = LocalDate.now(FUSO);

        return pedidoRepository.listarTodas().stream()
                .filter(pedido -> pedido.getEtaPrevista() != null)
                .filter(pedido -> !pedido.isEncerrado())
                .filter(pedido -> hoje.equals(pedido.getEtaPrevista().atZone(FUSO).toLocalDate()))
                .sorted(Comparator.comparing(Pedido::getEtaPrevista))
                .map(PedidoResponse::de)
                .toList();
    }

    private List<AlertaResponse> alertasRecentes() {
        return alertaRepository.listarNaoResolvidos().stream()
                .filter(alerta -> alerta.getStatus() == StatusAlerta.ATIVO)
                .sorted(Comparator.comparing(Alerta::getCriadoEm,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(ALERTAS_NO_RESUMO)
                .map(AlertaResponse::de)
                .toList();
    }
}
