package com.ralvesper.qualitygate;

public interface QualityCheck {
    GateResult execute(ProjectContext context);
}
