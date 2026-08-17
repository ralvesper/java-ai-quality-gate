package com.ralvesper.qualitygate;

import java.util.List;

public record QualityGateReport(
        String project,
        GateStatus status,
        List<GateResult> checks
) {
}
