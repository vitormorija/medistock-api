package br.com.medistock.api.repository;

import br.com.medistock.api.model.PontoSerie;
import br.com.medistock.api.model.PrevisaoDemanda;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.medistock.api.repository.ConversorFirestore.aguardar;
import static br.com.medistock.api.repository.ConversorFirestore.paraInstant;
import static br.com.medistock.api.repository.ConversorFirestore.paraInteiro;
import static br.com.medistock.api.repository.ConversorFirestore.paraTimestamp;

@Repository
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class PrevisaoRepositoryFirestore implements PrevisaoRepository {
    private static final String COLECAO = "previsoes";

    private final Firestore firestore;

    public PrevisaoRepositoryFirestore(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public PrevisaoDemanda salvar(PrevisaoDemanda previsao) {
        DocumentReference documento = firestore.collection(COLECAO).document();
        previsao.setId(documento.getId());
        aguardar(documento.set(paraMapa(previsao)));

        return previsao;
    }

    @Override
    public Optional<PrevisaoDemanda> buscarMaisRecentePorItem(String itemId) {
        return aguardar(firestore.collection(COLECAO)
                .whereEqualTo("itemId", itemId)
                .get()).getDocuments().stream()
                .map(this::paraPrevisao)
                .max(Comparator.comparing(PrevisaoDemanda::getGeradoEm));
    }

    private Map<String, Object> paraMapa(PrevisaoDemanda previsao) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("itemId", previsao.getItemId());
        dados.put("geradoEm", paraTimestamp(previsao.getGeradoEm()));
        dados.put("historico", paraListaDeMapas(previsao.getHistorico()));
        dados.put("previsao", paraListaDeMapas(previsao.getPrevisao()));
        dados.put("recomendacao", previsao.getRecomendacao());
        dados.put("fatoresConsiderados", previsao.getFatoresConsiderados());
        return dados;
    }

    private List<Map<String, Object>> paraListaDeMapas(List<PontoSerie> pontos) {
        return pontos.stream()
                .map(ponto -> {
                    Map<String, Object> dados = new HashMap<String, Object>();
                    dados.put("periodo", ponto.getPeriodo());
                    dados.put("valor", ponto.getValor());
                    return dados;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<PontoSerie> paraPontos(Object bruto) {
        List<PontoSerie> pontos = new ArrayList<>();

        if (bruto instanceof List<?> lista) {
            for (Object elemento : lista) {
                Map<String, Object> dados = (Map<String, Object>) elemento;
                pontos.add(PontoSerie.builder()
                        .periodo((String) dados.get("periodo"))
                        .valor(paraInteiro((Long) dados.get("valor")))
                        .build());
            }
        }
        return pontos;
    }

    @SuppressWarnings("unchecked")
    private PrevisaoDemanda paraPrevisao(DocumentSnapshot documento) {
        Object fatores = documento.get("fatoresConsiderados");

        return PrevisaoDemanda.builder()
                .id(documento.getId())
                .itemId(documento.getString("itemId"))
                .geradoEm(paraInstant(documento.getTimestamp("geradoEm")))
                .historico(paraPontos(documento.get("historico")))
                .previsao(paraPontos(documento.get("previsao")))
                .recomendacao(documento.getString("recomendacao"))
                .fatoresConsiderados(fatores instanceof List<?>
                        ? new ArrayList<>((List<String>) fatores)
                        : new ArrayList<>())
                .build();
    }
}
