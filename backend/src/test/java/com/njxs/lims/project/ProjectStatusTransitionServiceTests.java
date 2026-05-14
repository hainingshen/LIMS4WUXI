package com.njxs.lims.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.domain.ProjectStatusChangedEvent;
import com.njxs.lims.project.service.ProjectStatusTransitionService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectStatusTransitionServiceTests {

  private final List<Object> events = new ArrayList<>();
  private final ProjectStatusTransitionService transitionService = new ProjectStatusTransitionService(events::add);

  @Test
  void rejectsDirectTransitionFromAcceptedToQuoted() {
    Project project = project(ProjectStatus.ACCEPTED);

    assertThatThrownBy(() -> transitionService.transition(project, ProjectStatus.QUOTED, "skip quotation guard"))
        .isInstanceOfSatisfying(BusinessException.class, exception -> {
          assertThat(exception.getCode()).isEqualTo("ILLEGAL_PROJECT_STATUS_TRANSITION");
          assertThat(exception.getDetails()).containsEntry("source", "ACCEPTED");
          assertThat(exception.getDetails()).containsEntry("target", "QUOTED");
        });

    assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACCEPTED.name());
    assertThat(events).isEmpty();
  }

  @Test
  void allowsAcceptedToQuotableThenQuoted() {
    Project project = project(ProjectStatus.ACCEPTED);

    transitionService.transition(project, ProjectStatus.QUOTABLE, "standard project");
    transitionService.transition(project, ProjectStatus.QUOTED, "quotation issued");

    assertThat(project.getStatus()).isEqualTo(ProjectStatus.QUOTED.name());
    assertThat(events)
        .extracting(event -> ((ProjectStatusChangedEvent) event).targetStatus())
        .containsExactly(ProjectStatus.QUOTABLE.name(), ProjectStatus.QUOTED.name());
  }

  private Project project(ProjectStatus status) {
    Project project = new Project();
    project.setId(UUID.randomUUID());
    project.setStatus(status.name());
    return project;
  }
}
