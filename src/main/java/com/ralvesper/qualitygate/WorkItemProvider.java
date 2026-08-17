package com.ralvesper.qualitygate;

import java.io.IOException;
import java.util.Optional;

public interface WorkItemProvider {
    Optional<WorkItem> resolve(String workItemId) throws IOException, InterruptedException;
}
