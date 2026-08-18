package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.AlertaRequest;
import br.com.medistock.api.dto.response.AlertaResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Alerta;
import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;
import br.com.medistock.api.repository.AlertaRepository;
import br.com.medistock.api.repository.PedidoRepository;
import br.com.medistock.api.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AlertaService {
    private static final Comparator<Alerta> MAIS_RECENTES_PRIMEIRO =
            Comparator.comparing(Alerta::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder()));

    private final AlertaRepository repository;
    private final ItemRepository itemRepository;
    private final PedidoRepository pedidoRepository;

    public AlertaService(AlertaRepository repository,
                         ItemRepository itemRepository,
                         PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public List<AlertaResponse> listar(TipoAlerta tipo, SeveridadeAlerta severidade, StatusAlerta status) {
        return repository.listarTodos().stream()
                .filter(alerta -> tipo == null || alerta.getTipo() == tipo)
                .filter(alerta -> severidade == null || alerta.getSeveridade() == severidade)
                .filter(alerta -> status == null || alerta.getStatus() == status)
                .sorted(MAIS_RECENTES_PRIMEIRO)
                .map(AlertaResponse::de)
                .toList();
    }

    public AlertaResponse buscarPorId(String id) {
        return AlertaResponse.de(buscarEntidade(id));
    }

    public AlertaResponse criar(AlertaRequest request) {
        exigirReferenciasValidas(request.itemId(), request.pedidoId());

        Alerta alerta = Alerta.builder()
                .tipo(request.tipo())
                .severidade(request.severidade())
                .titulo(request.titulo())
                .mensagem(request.mensagem())
                .itemId(request.itemId())
                .pedidoId(request.pedidoId())
                .status(StatusAlerta.ATIVO)
                .criadoEm(Instant.now())
                .build();

        return AlertaResponse.de(repository.salvar(alerta));
    }

    public AlertaResponse resolver(String id) {
        return trocarStatus(id, StatusAlerta.RESOLVIDO);
    }

    public AlertaResponse ignorar(String id) {
        return trocarStatus(id, StatusAlerta.IGNORADO);
    }

    private AlertaResponse trocarStatus(String id, StatusAlerta status) {
        Alerta alerta = buscarEntidade(id);
        alerta.setStatus(status);
        alerta.setResolvidoEm(Instant.now());

        return AlertaResponse.de(repository.salvar(alerta));
    }

    public void remover(String id) {
        if (!repository.removerPorId(id)) {
            throw new RecursoNaoEncontradoException("Alerta não encontrado: " + id);
        }
    }

    public List<AlertaResponse> gerar() {
        Set<String> jaAbertos = new HashSet<>(repository.listarNaoResolvidos().stream()
                .map(this::chaveDeDuplicidade)
                .toList());

        List<Alerta> novos = new ArrayList<>();

        for (Item item : itemRepository.listarTodos()) {
            if (item.isEstoqueAbaixoDoMinimo()) {
                acrescentar(novos, jaAbertos, alertaDeEstoque(item));
            }
            if (item.isValidadeProxima()) {
                acrescentar(novos, jaAbertos, alertaDeValidade(item));
            }
        }

        for (Pedido pedido : pedidoRepository.listarTodas()) {
            if (pedido.isSlaExcedido()) {
                acrescentar(novos, jaAbertos, alertaDeLogistica(pedido));
            }
        }

        return novos.stream().map(repository::salvar).map(AlertaResponse::de).toList();
    }

    private void acrescentar(List<Alerta> novos, Set<String> jaAbertos, Alerta alerta) {
        if (jaAbertos.add(chaveDeDuplicidade(alerta))) {
            novos.add(alerta);
        }
    }

    private String chaveDeDuplicidade(Alerta alerta) {
        return alerta.getTipo() + "|" + alerta.getItemId() + "|" + alerta.getPedidoId();
    }

    private Alerta alertaDeEstoque(Item item) {
        boolean zerado = item.isEstoqueZerado();
        boolean critico = item.isEstoqueCritico();

        String prefixo;
        if (zerado) {
            prefixo = "Estoque zerado: ";
        } else if (critico) {
            prefixo = "Estoque crítico: ";
        } else {
            prefixo = "Estoque baixo: ";
        }

        return Alerta.builder()
                .tipo(TipoAlerta.ESTOQUE_CRITICO)
                .severidade(critico ? SeveridadeAlerta.CRITICO : SeveridadeAlerta.ATENCAO)
                .titulo(prefixo + item.getNome())
                .mensagem(zerado
                        ? "Não há unidades de %s em estoque. O mínimo definido é %d %s."
                                .formatted(item.getNome(), item.getQuantidadeMinima(), item.getUnidadeMedida())
                        : "Restam %d %s de %s, abaixo do mínimo de %d."
                                .formatted(item.getQuantidadeAtual(), item.getUnidadeMedida(),
                                        item.getNome(), item.getQuantidadeMinima()))
                .itemId(item.getId())
                .status(StatusAlerta.ATIVO)
                .criadoEm(Instant.now())
                .build();
    }

    private Alerta alertaDeValidade(Item item) {
        boolean vencido = item.isValidadeVencida();
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), item.getDataValidade());

        return Alerta.builder()
                .tipo(TipoAlerta.VALIDADE)
                .severidade(vencido ? SeveridadeAlerta.CRITICO : SeveridadeAlerta.ATENCAO)
                .titulo((vencido ? "Item vencido: " : "Validade próxima: ") + item.getNome())
                .mensagem(vencido
                        ? "%s venceu em %s, há %d dia(s), e deve ser retirado do estoque."
                                .formatted(item.getNome(), item.getDataValidade(), Math.abs(dias))
                        : "%s vence em %s, daqui a %d dia(s)."
                                .formatted(item.getNome(), item.getDataValidade(), dias))
                .itemId(item.getId())
                .status(StatusAlerta.ATIVO)
                .criadoEm(Instant.now())
                .build();
    }

    private Alerta alertaDeLogistica(Pedido pedido) {
        return Alerta.builder()
                .tipo(TipoAlerta.ATRASO_ENTREGA)
                .severidade(SeveridadeAlerta.ATENCAO)
                .titulo("Pedido atrasado: " + pedido.getCodigo())
                .mensagem("O pedido %s estava previsto para %s e continua com status %s."
                        .formatted(pedido.getCodigo(), pedido.getEtaPrevista(), pedido.getStatus()))
                .pedidoId(pedido.getId())
                .status(StatusAlerta.ATIVO)
                .criadoEm(Instant.now())
                .build();
    }

    private Alerta buscarEntidade(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado: " + id));
    }

    private void exigirReferenciasValidas(String itemId, String pedidoId) {
        if (itemId != null && itemRepository.buscarPorId(itemId).isEmpty()) {
            throw new RegraDeNegocioException("Item não encontrado: " + itemId);
        }
        if (pedidoId != null && pedidoRepository.buscarPorId(pedidoId).isEmpty()) {
            throw new RegraDeNegocioException("Pedido não encontrada: " + pedidoId);
        }
    }
}
