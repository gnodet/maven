package org.apache.maven.execution;

import java.util.List;
import org.apache.maven.api.Project;
import org.apache.maven.api.exec.BuildSummary;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.exec.MavenResult;
import org.apache.maven.internal.impl.InternalMavenSession;

public class DefaultMavenResult implements MavenResult {

    private final InternalMavenSession session;
    private final MavenExecutionResult executionResult;

    public DefaultMavenResult(InternalMavenSession session, MavenExecutionResult executionResult) {
        this.session = session;
        this.executionResult = executionResult;
    }

    @Override
    public MavenRequest getRequest() {
        return null;
    }

    @Override
    public BuildSummary getBuildSummary(Project project) {
        var summary = executionResult.getBuildSummary(session.getMavenProject(project));
        if (summary instanceof BuildSuccess) {
            return new org.apache.maven.api.exec.BuildSuccess(project, summary.getWallTime(), summary.getExecTime());
        } else if (summary instanceof BuildFailure failure) {
            return new org.apache.maven.api.exec.BuildFailure(project, summary.getWallTime(), summary.getExecTime(), failure.getCause());
        } else {
            return null;
        }
    }

    @Override
    public List<Throwable> getExceptions() {
        return List.copyOf(executionResult.getExceptions());
    }

    @Override
    public MavenResult withCanResume(boolean canResume) {
        return null;
    }

    @Override
    public List<Project> getTopologicallySortedProjects() {
        return List.copyOf(session.getProjects(executionResult.getTopologicallySortedProjects()));
    }

    public MavenExecutionResult getMavenExecutionResult() {
        return executionResult;
    }
}
