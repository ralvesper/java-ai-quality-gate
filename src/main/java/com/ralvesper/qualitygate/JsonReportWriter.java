package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.PrintStream;

public class JsonReportWriter implements ReportWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void write(QualityGateReport report, PrintStream out) throws Exception {
        out.println(mapper.writeValueAsString(report));
    }
}
