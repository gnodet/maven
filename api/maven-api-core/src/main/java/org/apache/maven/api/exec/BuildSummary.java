package org.apache.maven.api.exec;

import java.time.Duration;
import org.apache.maven.api.Project;

public interface BuildSummary {
    Project getProject();
    Duration getWallTime();
    Duration getExecTime();
}
