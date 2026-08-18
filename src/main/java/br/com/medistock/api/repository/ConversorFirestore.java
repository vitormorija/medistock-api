package br.com.medistock.api.repository;

import br.com.medistock.api.exception.FalhaDePersistenciaException;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

final class ConversorFirestore {

    private static final Logger LOG = LoggerFactory.getLogger(ConversorFirestore.class);
    private ConversorFirestore() {
    }

    static <T> T aguardar(ApiFuture<T> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new FalhaDePersistenciaException("Acesso ao Firestore interrompido", excecao);
        } catch (ExecutionException excecao) {
            throw new FalhaDePersistenciaException("Falha ao acessar o Firestore", excecao);
        }
    }

    static Timestamp paraTimestamp(Instant instante) {
        return instante == null ? null : Timestamp.ofTimeSecondsAndNanos(
                instante.getEpochSecond(), instante.getNano());
    }

    static Instant paraInstant(Timestamp marca) {
        return marca == null ? null : Instant.ofEpochSecond(marca.getSeconds(), marca.getNanos());
    }

    static String paraTexto(LocalDate data) {
        return data == null ? null : data.toString();
    }

    static LocalDate paraData(String texto) {
        return texto == null ? null : LocalDate.parse(texto);
    }

    static Integer paraInteiro(Long codigo) {
        return codigo == null ? null : codigo.intValue();
    }

    static Double paraNumero(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    static BigDecimal paraDecimal(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    static <E extends Enum<E>> E paraEnum(Class<E> tipo, String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException valorDesconhecido) {
            LOG.warn("Valor de {} nao reconhecido no Firestore: {}", tipo.getSimpleName(), valor);
            return null;
        }
    }

    static String paraTexto(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}
