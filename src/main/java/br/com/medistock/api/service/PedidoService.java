package br.com.medistock.api.service;

import br.com.medistock.api.dto.request.AtualizacaoStatusRequest;
import br.com.medistock.api.dto.request.PedidoRequest;
import br.com.medistock.api.dto.request.ItemPedidoRequest;
import br.com.medistock.api.dto.response.PedidoResponse;
import br.com.medistock.api.exception.RecursoNaoEncontradoException;
import br.com.medistock.api.exception.RegraDeNegocioException;
import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.Fornecedor;
import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.ItemPedido;
import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.enums.StatusPedido;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import br.com.medistock.api.repository.PedidoRepository;
import br.com.medistock.api.repository.FornecedorRepository;
import br.com.medistock.api.repository.ItemRepository;
import br.com.medistock.api.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class PedidoService {
    private static final String FORMATO_NUMERO = "#%04d";

    private static final Comparator<Pedido> ORDEM_PADRAO =
            Comparator.comparing(Pedido::getCodigo, Comparator.reverseOrder());

    private final PedidoRepository repository;
    private final FornecedorRepository fornecedorRepository;
    private final ItemRepository itemRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public PedidoService(PedidoRepository repository,
                          FornecedorRepository fornecedorRepository,
                          ItemRepository itemRepository,
                          MovimentacaoRepository movimentacaoRepository) {
        this.repository = repository;
        this.fornecedorRepository = fornecedorRepository;
        this.itemRepository = itemRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<PedidoResponse> listar(StatusPedido status, String fornecedorId) {
        return repository.listarTodas().stream()
                .filter(pedido -> status == null || pedido.getStatus() == status)
                .filter(pedido -> fornecedorId == null || fornecedorId.equals(pedido.getFornecedorId()))
                .sorted(ORDEM_PADRAO)
                .map(PedidoResponse::de)
                .toList();
    }

    public List<PedidoResponse> listarAtrasados() {
        return repository.listarTodas().stream()
                .filter(Pedido::isSlaExcedido)
                .sorted(ORDEM_PADRAO)
                .map(PedidoResponse::de)
                .toList();
    }

    public PedidoResponse buscarPorId(String id) {
        return PedidoResponse.de(buscarEntidade(id));
    }

    public PedidoResponse criar(PedidoRequest request) {
        Fornecedor fornecedor = exigirFornecedorAtivo(request.fornecedorId());
        request.itens().forEach(item -> exigirItemExistente(item.itemId()));

        List<ItemPedido> itens = request.itens().stream().map(this::paraItem).toList();

        Instant agora = Instant.now();
        Pedido pedido = Pedido.builder()
                .codigo(gerarProximoCodigo())
                .fornecedorId(fornecedor.getId())
                .status(StatusPedido.PENDENTE)
                .dataPedido(agora)
                .etaPrevista(request.etaPrevista())
                .slaHoras(request.slaHoras() == null ? fornecedor.getSlaHoras() : request.slaHoras())
                .itens(itens)
                .valorTotal(somarItens(itens))
                .build();

        return PedidoResponse.de(repository.salvar(pedido));
    }

    public PedidoResponse atualizarStatus(String id, AtualizacaoStatusRequest request) {
        Pedido pedido = buscarEntidade(id);

        if (request.status() == StatusPedido.ENTREGUE) {
            throw new RegraDeNegocioException(
                    "Use POST /api/v1/pedidos/" + id + "/confirmar para registrar o recebimento, "
                            + "porque a confirmação também dá entrada no estoque");
        }

        pedido.setStatus(request.status());
        return PedidoResponse.de(repository.salvar(pedido));
    }

    public PedidoResponse confirmar(String id, String usuarioId) {
        Pedido pedido = buscarEntidade(id);

        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new RegraDeNegocioException("O pedido " + pedido.getCodigo() + " já foi confirmado");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraDeNegocioException("O pedido " + pedido.getCodigo() + " está cancelado");
        }

        Instant agora = Instant.now();
        for (ItemPedido linha : pedido.getItens()) {
            Item item = exigirItemExistente(linha.getItemId());
            int resultante = item.getQuantidadeAtual() + linha.getQuantidade();

            item.setQuantidadeAtual(resultante);
            item.setAtualizadoEm(agora);
            itemRepository.salvar(item);

            movimentacaoRepository.salvar(MovimentacaoEstoque.builder()
                    .itemId(item.getId())
                    .tipo(TipoMovimentacao.ENTRADA)
                    .quantidade(linha.getQuantidade())
                    .quantidadeResultante(resultante)
                    .usuarioId(usuarioId)
                    .pedidoId(pedido.getId())
                    .dataHora(agora)
                    .build());
        }

        pedido.setStatus(StatusPedido.ENTREGUE);
        pedido.setDataEntrega(agora);

        return PedidoResponse.de(repository.salvar(pedido));
    }

    private Pedido buscarEntidade(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrada: " + id));
    }

    private Fornecedor exigirFornecedorAtivo(String fornecedorId) {
        Fornecedor fornecedor = fornecedorRepository.buscarPorId(fornecedorId)
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Fornecedor não encontrado: " + fornecedorId));

        if (!fornecedor.isAtivo()) {
            throw new RegraDeNegocioException("O fornecedor " + fornecedor.getNome()
                    + " está inativo e não pode receber novos pedidos");
        }
        return fornecedor;
    }

    private Item exigirItemExistente(String itemId) {
        return itemRepository.buscarPorId(itemId)
                .orElseThrow(() -> new RegraDeNegocioException("Item não encontrado: " + itemId));
    }

    private String gerarProximoCodigo() {
        long proximo = repository.buscarUltimoCodigo()
                .map(codigo -> Long.parseLong(codigo.replace("#", "")) + 1)
                .orElse(1L);

        return FORMATO_NUMERO.formatted(proximo);
    }

    private ItemPedido paraItem(ItemPedidoRequest request) {
        return ItemPedido.builder()
                .itemId(request.itemId())
                .quantidade(request.quantidade())
                .valorUnitario(request.valorUnitario())
                .build();
    }

    private BigDecimal somarItens(List<ItemPedido> itens) {
        BigDecimal total = null;

        for (ItemPedido linha : itens) {
            if (linha.getValorUnitario() == null) {
                continue;
            }
            BigDecimal subtotal = linha.getValorUnitario()
                    .multiply(BigDecimal.valueOf(linha.getQuantidade()));
            total = total == null ? subtotal : total.add(subtotal);
        }

        return total;
    }
}
