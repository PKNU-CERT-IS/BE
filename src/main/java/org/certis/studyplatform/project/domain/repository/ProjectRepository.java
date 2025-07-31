package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.model.Project;
import org.certis.studyplatform.project.domain.vo.ProjectId;
import org.certis.studyplatform.shared.domain.vo.MemberId;
import java.util.Optional;
import java.util.List;

public interface ProjectRepository {
    Optional<Project> findById(ProjectId id);
    List<Project> findByMemberId(MemberId memberId);
    List<Project> findByCategory(String category);
    List<Project> findByDifficulty(String difficulty);
    Project save(Project project);
    void deleteById(ProjectId id);
}