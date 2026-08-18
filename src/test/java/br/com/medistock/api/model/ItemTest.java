package br.com.medistock.api.model;

import br.com.medistock.api.model.enums.StatusItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Item: status derivado da quantidade, e validade à parte")
class ItemTest {
    private static Item item(int atual, int minimo, LocalDate validade) {
        return Item.builder()
                .quantidadeAtual(atual)
                .quantidadeMinima(minimo)
                .dataValidade(validade)
                .build();
    }

    private static final LocalDate LONGE = LocalDate.now().plusYears(1);

    @Nested
    @DisplayName("getStatus")
    class Status {
        @Test
        @DisplayName("é NORMAL acima do mínimo")
        void normal() {
            assertEquals(StatusItem.NORMAL, item(50, 10, LONGE).getStatus());
        }

        @Test
        @DisplayName("é ATENCAO quando a quantidade iguala o mínimo")
        void atencaoNoLimite() {
            assertEquals(StatusItem.ATENCAO, item(10, 10, LONGE).getStatus());
        }

        @Test
        @DisplayName("é ATENCAO entre 30% do mínimo e o mínimo")
        void atencaoNaFaixaIntermediaria() {
            assertEquals(StatusItem.ATENCAO, item(4, 10, LONGE).getStatus());
        }

        @Test
        @DisplayName("vira CRITICO em 30% do mínimo, a borda entre os dois")
        void criticoNaBordaDaFracao() {
            assertEquals(StatusItem.CRITICO, item(3, 10, LONGE).getStatus());
        }

        @Test
        @DisplayName("estoque zerado é sempre CRITICO")
        void zeradoEhCritico() {
            assertEquals(StatusItem.CRITICO, item(0, 10, LONGE).getStatus());
        }

        @Test
        @DisplayName("a validade não altera o status, nem quando já venceu")
        void validadeNaoEntraNoStatus() {
            assertEquals(StatusItem.NORMAL,
                    item(50, 10, LocalDate.now().minusDays(5)).getStatus());
        }
    }

    @Nested
    @DisplayName("janela de validade")
    class Validade {
        @Test
        @DisplayName("o trigésimo dia ainda conta como próximo")
        void bordaDentro() {
            assertTrue(item(50, 10, LocalDate.now().plusDays(30)).isValidadeProxima());
        }

        @Test
        @DisplayName("o trigésimo primeiro já está fora")
        void bordaFora() {
            assertFalse(item(50, 10, LocalDate.now().plusDays(31)).isValidadeProxima());
        }

        @Test
        @DisplayName("data passada continua próxima: vencido não deixa de ser problema")
        void vencidoContinuaContando() {
            Item vencido = item(50, 10, LocalDate.now().minusDays(5));

            assertTrue(vencido.isValidadeProxima());
            assertTrue(vencido.isValidadeVencida());
        }

        @Test
        @DisplayName("distingue vencido de apenas próximo")
        void vencidoOuProximo() {
            assertFalse(item(50, 10, LocalDate.now().plusDays(10)).isValidadeVencida());
            assertTrue(item(50, 10, LocalDate.now().plusDays(10)).isValidadeProxima());
        }
    }

    @Test
    @DisplayName("quantidade e validade são independentes: o item acusa as duas")
    void condicoesIndependentes() {
        Item criticoEVencendo = item(2, 10, LocalDate.now().plusDays(5));

        assertTrue(criticoEVencendo.isEstoqueCritico());
        assertTrue(criticoEVencendo.isValidadeProxima());
        assertEquals(StatusItem.CRITICO, criticoEVencendo.getStatus(),
                "o status não carrega a validade, por isso os alertas usam as condições separadas");
    }

    @Test
    @DisplayName("distingue estoque zerado de estoque apenas baixo")
    void zeradoOuBaixo() {
        assertTrue(item(0, 10, LONGE).isEstoqueZerado());
        assertFalse(item(5, 10, LONGE).isEstoqueZerado());
    }

    @Test
    @DisplayName("abaixo do mínimo e crítico são patamares diferentes")
    void doisPatamares() {
        Item emAtencao = item(8, 10, LONGE);

        assertTrue(emAtencao.isEstoqueAbaixoDoMinimo());
        assertFalse(emAtencao.isEstoqueCritico());
    }
}
