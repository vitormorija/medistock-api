package br.com.medistock.api.repository;

import br.com.medistock.api.model.Pedido;
import br.com.medistock.api.model.ItemPedido;
import br.com.medistock.api.model.enums.StatusPedido;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraEnum;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraDecimal;
import static br.com.medistock.api.repository.ConversorFirestore.paraInteiro;
import static br.com.medistock.api.repository.ConversorFirestore.paraNumero;
import static br.com.medistock.api.repository.ConversorFirestore.paraTexto;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class PedidoRepositoryFirestore implements PedidoRepository {
    private static final String COLECAO = "pedidos";

    private final Firestore firestore;

    public PedidoRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        DocumentReference documento = pedido.getId() == null
                ? firestore.collection(COLECAO).document()
                : firestore.collection(COLECAO).document(pedido.getId());

        pedido.setId(documento.getId());
        aguardar(documento.set(paraMapa(pedido)));

        return pedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(String id) {
        DocumentSnapshot documento = aguardar(firestore.collection(COLECAO).document(id).get());

        return documento.exists() ? Optional.of(paraPedido(documento)) : Optional.empty();
    }

    @Override
    public List<Pedido> listarTodas() {
        return aguardar(firestore.collection(COLECAO).get()).getDocuments().stream()
                .map(this::paraPedido)
                .map(Pedido.class::cast)
                .toList();
    }

    @Override
    public Optional<String> buscarUltimoCodigo() {
        List<QueryDocumentSnapshot> encontradas = aguardar(firestore.collection(COLECAO)
                .orderBy("codigo", Query.Direction.DESCENDING)
                .limit(1)
                .get()).getDocuments();

        return encontradas.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(encontradas.getFirst().getString("codigo"));
    }

    private Map<String, Object> paraMapa(Pedido pedido) {
        List<Map<String, Object>> itens = pedido.getItens().stream()
                .map(item -> {
                    Map<String, Object> dados = new HashMap<String, Object>();
                    dados.put("itemId", item.getItemId());
                    dados.put("quantidade", item.getQuantidade());
                    dados.put("valorUnitario", paraNumero(item.getValorUnitario()));
                    return dados;
                })
                .toList();

        Map<String, Object> dados = new HashMap<>();
        dados.put("codigo", pedido.getCodigo());
        dados.put("fornecedorId", pedido.getFornecedorId());
        dados.put("status", paraTexto(pedido.getStatus()));
        dados.put("dataPedido", paraTimestamp(pedido.getDataPedido()));
        dados.put("etaPrevista", paraTimestamp(pedido.getEtaPrevista()));
        dados.put("dataEntrega", paraTimestamp(pedido.getDataEntrega()));
        dados.put("reentregaPrevistaEm", paraTimestamp(pedido.getReentregaPrevistaEm()));
        dados.put("slaHoras", pedido.getSlaHoras());
        dados.put("valorTotal", paraNumero(pedido.getValorTotal()));
        dados.put("valorReembolso", paraNumero(pedido.getValorReembolso()));
        dados.put("motivoAtraso", pedido.getMotivoAtraso());
        dados.put("motivoOcorrencia", pedido.getMotivoOcorrencia());
        dados.put("itens", itens);
        return dados;
    }

    @SuppressWarnings("unchecked")
    private Pedido paraPedido(DocumentSnapshot documento) {
        List<ItemPedido> itens = new ArrayList<>();
        Object bruto = documento.get("itens");

        if (bruto instanceof List<?> lista) {
            for (Object elemento : lista) {
                Map<String, Object> dados = (Map<String, Object>) elemento;
                itens.add(ItemPedido.builder()
                        .itemId((String) dados.get("itemId"))
                        .quantidade(paraInteiro((Long) dados.get("quantidade")))
                        .valorUnitario(paraDecimal((Double) dados.get("valorUnitario")))
                        .build());
            }
        }

        return Pedido.builder()
                .id(documento.getId())
                .codigo(documento.getString("codigo"))
                .fornecedorId(documento.getString("fornecedorId"))
                .status(paraEnum(StatusPedido.class, documento.getString("status")))
                .dataPedido(paraInstant(documento.getTimestamp("dataPedido")))
                .etaPrevista(paraInstant(documento.getTimestamp("etaPrevista")))
                .dataEntrega(paraInstant(documento.getTimestamp("dataEntrega")))
                .reentregaPrevistaEm(paraInstant(documento.getTimestamp("reentregaPrevistaEm")))
                .slaHoras(paraInteiro(documento.getLong("slaHoras")))
                .valorTotal(paraDecimal(documento.getDouble("valorTotal")))
                .valorReembolso(paraDecimal(documento.getDouble("valorReembolso")))
                .motivoAtraso(documento.getString("motivoAtraso"))
                .motivoOcorrencia(documento.getString("motivoOcorrencia"))
                .itens(itens)
                .build();
    }
}
