package br.com.medistock.api.repository;

import br.com.medistock.api.model.MovimentacaoEstoque;
import br.com.medistock.api.model.enums.TipoMovimentacao;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraEnum;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraInteiro;
import static br.com.medistock.api.repository.ConversorFirestore.paraTexto;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class MovimentacaoRepositoryFirestore implements MovimentacaoRepository {
    private static final String COLECAO = "movimentacoes";

    private final Firestore firestore;

    public MovimentacaoRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao) {
        DocumentReference documento = firestore.collection(COLECAO).document();
        movimentacao.setId(documento.getId());
        aguardar(documento.set(paraMapa(movimentacao)));

        return movimentacao;
    }

    @Override
    public List<MovimentacaoEstoque> listarPorItem(String itemId) {
        return aguardar(firestore.collection(COLECAO)
                .whereEqualTo("itemId", itemId)
                .get()).getDocuments().stream()
                .map(this::paraMovimentacao)
                .map(MovimentacaoEstoque.class::cast)
                .toList();
    }

    private Map<String, Object> paraMapa(MovimentacaoEstoque movimentacao) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("itemId", movimentacao.getItemId());
        dados.put("tipo", paraTexto(movimentacao.getTipo()));
        dados.put("quantidade", movimentacao.getQuantidade());
        dados.put("quantidadeResultante", movimentacao.getQuantidadeResultante());
        dados.put("usuarioId", movimentacao.getUsuarioId());
        dados.put("pedidoId", movimentacao.getPedidoId());
        dados.put("dataHora", paraTimestamp(movimentacao.getDataHora()));
        return dados;
    }

    private MovimentacaoEstoque paraMovimentacao(DocumentSnapshot documento) {
        return MovimentacaoEstoque.builder()
                .id(documento.getId())
                .itemId(documento.getString("itemId"))
                .tipo(paraEnum(TipoMovimentacao.class, documento.getString("tipo")))
                .quantidade(paraInteiro(documento.getLong("quantidade")))
                .quantidadeResultante(paraInteiro(documento.getLong("quantidadeResultante")))
                .usuarioId(documento.getString("usuarioId"))
                .pedidoId(documento.getString("pedidoId"))
                .dataHora(paraInstant(documento.getTimestamp("dataHora")))
                .build();
    }
}
