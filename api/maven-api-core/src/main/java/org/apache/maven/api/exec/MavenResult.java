package org.apache.maven.api.exec;

import java.util.List;
import org.apache.maven.api.Project;

public interface MavenResult {

    MavenRequest getRequest();

    /**
     * Gets the build summary for the specified project.
     *
     * @param project The project to get the build summary for, must not be {@code null}.
     * @return The build summary for the project or {@code null} if the project has not been built (yet).
     */
    BuildSummary getBuildSummary(Project project);

    List<Throwable> getExceptions();

    MavenResult withCanResume(boolean canResume);

    List<Project> getTopologicallySortedProjects();

    void addBuildSummary(BuildSummary buildFailure);

    void addException(Throwable t);
}
