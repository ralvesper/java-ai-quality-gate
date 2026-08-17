package com.ralvesper.qualitygate;

import java.util.ArrayList;
import java.util.List;

public class QualityGateEngine {

    private final List<QualityCheck> checks;

    public QualityGateEngine(List<QualityCheck> checks) {
        this.checks = List.copyOf(checks);
    }

    public QualityGateReport execute(ProjectContext context) {
        List<GateResult> results = new ArrayList<>();
        GateStatus consolidated = GateStatus.PASS;

        for (QualityCheck check : checks) {
            GateResult result = check.execute(context);
            results.add(result);
            consolidated = consolidate(consolidated, result.status());
        }

        return new QualityGateReport(context.projectPath().toString(), consolidated, results);
    }

    private GateStatus consolidate(GateStatus current, GateStatus next) {
        if (next == GateStatus.ERROR || next == GateStatus.FAIL) {
            return next;
        }
        if (next == GateStatus.WARNING && current == GateStatus.PASS) {
            return GateStatus.WARNING;
        }
        return current;
    }
}
