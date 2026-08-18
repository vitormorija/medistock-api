package br.com.medistock.api.repository;

import br.com.medistock.api.model.enums.TipoAlerta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ConversorFirestore: leitura tolerante do banco")
class ConversorFirestoreTest {

    @Test
    @DisplayName("converte o valor conhecido")
    void valorConhecido() {
        assertEquals(TipoAlerta.ESTOQUE_CRITICO,
                ConversorFirestore.paraEnum(TipoAlerta.class, "ESTOQUE_CRITICO"));
    }

    @Test
    @DisplayName("valor ausente vira nulo")
    void valorAusente() {
        assertNull(ConversorFirestore.paraEnum(TipoAlerta.class, null));
    }

    @Test
    @DisplayName("valor que o codigo nao conhece vira nulo em vez de derrubar a leitura")
    void valorDesconhecido() {
        assertNull(ConversorFirestore.paraEnum(TipoAlerta.class, "ESTOQUE"));
    }
}
