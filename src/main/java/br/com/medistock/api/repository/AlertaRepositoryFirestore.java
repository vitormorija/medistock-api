package br.com.medistock.api.repository;

import br.com.medistock.api.model.Alerta;
import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;
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
import static br.com.medistock.api.repository.ConversorFirestore.paraEnum;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraTexto;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class AlertaRepositoryFirestore implements AlertaRepository {
    private static final String COLECAO = "alertas";

    private final Firestore firestore;

    public AlertaRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Alerta salvar(Alerta alerta) {
        DocumentReference documento = alerta.getId() == null
                ? firestore.collection(COLECAO).document()
                : firestore.collection(COLECAO).document(alerta.getId());

        alerta.setId(documento.getId());
        aguardar(documento.set(paraMapa(alerta)));

        return alerta;
    }

    @Override
    public Optional<Alerta> buscarPorId(String id) {
        DocumentSnapshot documento = aguardar(firestore.collection(COLECAO).document(id).get());

        return documento.exists() ? Optional.of(paraAlerta(documento)) : Optional.empty();
    }

    @Override
    public List<Alerta> listarTodos() {
        return aguardar(firestore.collection(COLECAO).get()).getDocuments().stream()
                .map(this::paraAlerta)
                .map(Alerta.class::cast)
                .toList();
    }

    @Override
    public List<Alerta> listarNaoResolvidos() {
        return aguardar(firestore.collection(COLECAO)
                .whereIn("status", List.of(StatusAlerta.ATIVO.name(), StatusAlerta.IGNORADO.name()))
                .get()).getDocuments().stream()
                .map(this::paraAlerta)
                .map(Alerta.class::cast)
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

    private Map<String, Object> paraMapa(Alerta alerta) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("tipo", paraTexto(alerta.getTipo()));
        dados.put("severidade", paraTexto(alerta.getSeveridade()));
        dados.put("titulo", alerta.getTitulo());
        dados.put("mensagem", alerta.getMensagem());
        dados.put("itemId", alerta.getItemId());
        dados.put("pedidoId", alerta.getPedidoId());
        dados.put("status", paraTexto(alerta.getStatus()));
        dados.put("acaoTomada", alerta.getAcaoTomada());
        dados.put("criadoEm", paraTimestamp(alerta.getCriadoEm()));
        dados.put("resolvidoEm", paraTimestamp(alerta.getResolvidoEm()));
        return dados;
    }

    private Alerta paraAlerta(DocumentSnapshot documento) {
        return Alerta.builder()
                .id(documento.getId())
                .tipo(paraEnum(TipoAlerta.class, documento.getString("tipo")))
                .severidade(paraEnum(SeveridadeAlerta.class, documento.getString("severidade")))
                .titulo(documento.getString("titulo"))
                .mensagem(documento.getString("mensagem"))
                .itemId(documento.getString("itemId"))
                .pedidoId(documento.getString("pedidoId"))
                .status(statusOuAtivo(documento.getString("status")))
                .acaoTomada(documento.getString("acaoTomada"))
                .criadoEm(paraInstant(documento.getTimestamp("criadoEm")))
                .resolvidoEm(paraInstant(documento.getTimestamp("resolvidoEm")))
                .build();
    }

    private StatusAlerta statusOuAtivo(String valor) {
        return valor == null ? StatusAlerta.ATIVO : StatusAlerta.valueOf(valor);
    }
}
