package com.ralvesper.qualitygate;

import java.io.PrintStream;

public class TextReportWriter implements ReportWriter {
    @Override
    public void write(QualityGateReport report, PrintStream out) {
        out.println();
        out.println("Java AI Quality Gate");
        out.println("--------------------");
        out.println("Project: " + report.project());
        for (GateResult result : report.checks()) {
            out.printf("%s: %s (%d ms)%n", result.gate(), result.status(), result.durationMs());
            out.println("Message: " + result.message());
        }
        out.println();
        out.println("QUALITY GATE: " + (report.status().blocksMerge() ? "BLOCK" : "PASS"));
    }
}
