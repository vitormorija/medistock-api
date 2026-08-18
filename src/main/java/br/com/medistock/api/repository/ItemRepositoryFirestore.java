package br.com.medistock.api.repository;

import br.com.medistock.api.model.Item;
import br.com.medistock.api.model.enums.TipoItem;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraData;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraInteiro;
import static br.com.medistock.api.repository.ConversorFirestore.paraTexto;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class ItemRepositoryFirestore implements ItemRepository {
    private static final String COLECAO = "itens";

    private final Firestore firestore;

    public ItemRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Item salvar(Item item) {
        DocumentReference documento = item.getId() == null
                ? firestore.collection(COLECAO).document()
                : firestore.collection(COLECAO).document(item.getId());

        item.setId(documento.getId());
        aguardar(documento.set(paraMapa(item)));

        return item;
    }

    @Override
    public Optional<Item> buscarPorId(String id) {
        DocumentSnapshot documento = aguardar(firestore.collection(COLECAO).document(id).get());

        return documento.exists() ? Optional.of(paraItem(documento)) : Optional.empty();
    }

    @Override
    public List<Item> listarTodos() {
        return aguardar(firestore.collection(COLECAO).get()).getDocuments().stream()
                .map(this::paraItem)
                .map(Item.class::cast)
                .toList();
    }

    @Override
    public boolean removerPorId(String id) {
        DocumentReference documento = firestore.collection(COLECAO).document(id);

        if (!aguardar(documento.get()).exists()) {
            return false;
        }
        aguardar(documento.delete());
        return true;
    }

    private Map<String, Object> paraMapa(Item item) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", item.getNome());
        dados.put("descricao", item.getDescricao());
        dados.put("categoria", item.getCategoria());
        dados.put("unidadeMedida", item.getUnidadeMedida());
        dados.put("quantidadeAtual", item.getQuantidadeAtual());
        dados.put("quantidadeMinima", item.getQuantidadeMinima());
        dados.put("quantidadeMaxima", item.getQuantidadeMaxima());
        dados.put("quantidadeRecomendadaIa", item.getQuantidadeRecomendadaIa());
        dados.put("localArmazenamento", item.getLocalArmazenamento());
        dados.put("fornecedorId", item.getFornecedorId());
        dados.put("tipo", paraTexto(item.getTipo()));
        dados.put("lote", item.getLote());
        dados.put("dataValidade", paraTexto(item.getDataValidade()));
        dados.put("criadoEm", paraTimestamp(item.getCriadoEm()));
        dados.put("atualizadoEm", paraTimestamp(item.getAtualizadoEm()));
        return dados;
    }

    private Item paraItem(DocumentSnapshot documento) {
        return Item.builder()
                .id(documento.getId())
                .nome(documento.getString("nome"))
                .descricao(documento.getString("descricao"))
                .categoria(documento.getString("categoria"))
                .unidadeMedida(documento.getString("unidadeMedida"))
                .quantidadeAtual(paraInteiro(documento.getLong("quantidadeAtual")))
                .quantidadeMinima(paraInteiro(documento.getLong("quantidadeMinima")))
                .quantidadeMaxima(paraInteiro(documento.getLong("quantidadeMaxima")))
                .quantidadeRecomendadaIa(paraInteiro(documento.getLong("quantidadeRecomendadaIa")))
                .localArmazenamento(documento.getString("localArmazenamento"))
                .fornecedorId(documento.getString("fornecedorId"))
                .tipo(tipoOuPrimordial(documento.getString("tipo")))
                .lote(documento.getString("lote"))
                .dataValidade(paraData(documento.getString("dataValidade")))
                .criadoEm(paraInstant(documento.getTimestamp("criadoEm")))
                .atualizadoEm(paraInstant(documento.getTimestamp("atualizadoEm")))
                .build();
    }

    private TipoItem tipoOuPrimordial(String valor) {
        return valor == null ? TipoItem.PRIMORDIAL : TipoItem.valueOf(valor);
    }
}
