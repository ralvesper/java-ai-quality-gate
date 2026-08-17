package com.ralvesper.qualitygate;

import java.io.PrintStream;

public interface ReportWriter {
    void write(QualityGateReport report, PrintStream out) throws Exception;
}
