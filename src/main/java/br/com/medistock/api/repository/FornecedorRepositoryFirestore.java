package br.com.medistock.api.repository;

import br.com.medistock.api.model.Fornecedor;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraInteiro;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class FornecedorRepositoryFirestore implements FornecedorRepository {
    private static final String COLECAO = "fornecedores";

    private final Firestore firestore;

    public FornecedorRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Fornecedor salvar(Fornecedor fornecedor) {
        DocumentReference documento = fornecedor.getId() == null
                ? firestore.collection(COLECAO).document()
                : firestore.collection(COLECAO).document(fornecedor.getId());

        fornecedor.setId(documento.getId());
        aguardar(documento.set(paraMapa(fornecedor)));

        return fornecedor;
    }

    @Override
    public Optional<Fornecedor> buscarPorId(String id) {
        DocumentSnapshot documento = aguardar(firestore.collection(COLECAO).document(id).get());

        return documento.exists() ? Optional.of(paraFornecedor(documento)) : Optional.empty();
    }

    @Override
    public Optional<Fornecedor> buscarPorCnpj(String cnpj) {
        List<QueryDocumentSnapshot> encontrados = aguardar(firestore.collection(COLECAO)
                .whereEqualTo("cnpj", cnpj)
                .limit(1)
                .get()).getDocuments();

        return encontrados.isEmpty()
                ? Optional.empty()
                : Optional.of(paraFornecedor(encontrados.getFirst()));
    }

    @Override
    public List<Fornecedor> listarTodos() {
        return aguardar(firestore.collection(COLECAO).get()).getDocuments().stream()
                .map(this::paraFornecedor)
                .map(Fornecedor.class::cast)
                .toList();
    }

    private Map<String, Object> paraMapa(Fornecedor fornecedor) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", fornecedor.getNome());
        dados.put("cnpj", fornecedor.getCnpj());
        dados.put("email", fornecedor.getEmail());
        dados.put("telefone", fornecedor.getTelefone());
        dados.put("slaHoras", fornecedor.getSlaHoras());
        dados.put("scoreConfiabilidade", fornecedor.getScoreConfiabilidade());
        dados.put("historicoAtrasos", fornecedor.getHistoricoAtrasos());
        dados.put("ativo", fornecedor.isAtivo());
        return dados;
    }

    private Fornecedor paraFornecedor(DocumentSnapshot documento) {
        return Fornecedor.builder()
                .id(documento.getId())
                .nome(documento.getString("nome"))
                .cnpj(documento.getString("cnpj"))
                .email(documento.getString("email"))
                .telefone(documento.getString("telefone"))
                .slaHoras(paraInteiro(documento.getLong("slaHoras")))
                .scoreConfiabilidade(paraInteiro(documento.getLong("scoreConfiabilidade")))
                .historicoAtrasos(paraInteiro(documento.getLong("historicoAtrasos")))
                .ativo(Boolean.TRUE.equals(documento.getBoolean("ativo")))
                .build();
    }
}
