package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityGateEngineTest {

    @Test
    void shouldConsolidateWarningWithoutBlocking() {
        QualityCheck pass = context -> new GateResult("pass", GateStatus.PASS, "ok");
        QualityCheck warning = context -> new GateResult("warning", GateStatus.WARNING, "attention");

        QualityGateReport report = new QualityGateEngine(List.of(pass, warning))
                .execute(new ProjectContext(Path.of(".")));

        assertEquals(GateStatus.WARNING, report.status());
    }

    @Test
    void shouldBlockOnFailure() {
        QualityCheck fail = context -> new GateResult("fail", GateStatus.FAIL, "broken");

        QualityGateReport report = new QualityGateEngine(List.of(fail))
                .execute(new ProjectContext(Path.of(".")));

        assertEquals(GateStatus.FAIL, report.status());
    }

    @Test
    void shouldBlockOnInfrastructureError() {
        QualityCheck error = context -> new GateResult("error", GateStatus.ERROR, "tool missing");

        QualityGateReport report = new QualityGateEngine(List.of(error))
                .execute(new ProjectContext(Path.of(".")));

        assertEquals(GateStatus.ERROR, report.status());
    }
}
