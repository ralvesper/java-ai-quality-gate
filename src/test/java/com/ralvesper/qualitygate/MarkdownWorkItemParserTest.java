package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownWorkItemParserTest {

    @Test
    void parsesPluxeeIssueSections() {
        String body = """
                ## Contexto

                Boleto pendente não aparece na tela.

                ## Objetivo

                Exibir boletos pendentes corretamente.

                ## Critérios de aceite

                - [ ] boleto pendente deve aparecer
                - [x] outros clientes não devem mudar

                ## Cenários de teste

                ### Sucesso
                Validar boleto pendente.

                ### Regressão
                Validar cliente sem boleto.
                """;

        WorkItem item = new MarkdownWorkItemParser().parse(
                "FS-687",
                "[FS-687] Corrigir boletos",
                body,
                "github:ralvesper/pluxee-issues",
                "https://example.test/issues/1"
        );

        assertEquals("Boleto pendente não aparece na tela.", item.context());
        assertEquals("Exibir boletos pendentes corretamente.", item.objective());
        assertEquals(2, item.acceptanceCriteria().size());
        assertEquals("boleto pendente deve aparecer", item.acceptanceCriteria().get(0));
        assertEquals(2, item.testScenarios().size());
        assertEquals("Sucesso", item.testScenarios().get(0));
    }
}
