package com.njxs.lims.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class AuditAnnotationCoverageTests {

  @Test
  void businessWriteEndpointsRequireAuditedAnnotation() throws ClassNotFoundException {
    List<String> missingAudits = new ArrayList<>();
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

    for (var beanDefinition : scanner.findCandidateComponents("com.njxs.lims")) {
      Class<?> controllerClass = ClassUtils.forName(
          beanDefinition.getBeanClassName(),
          Thread.currentThread().getContextClassLoader()
      );
      if (controllerClass.getPackageName().startsWith("com.njxs.lims.auth.")) {
        continue;
      }
      for (Method method : controllerClass.getDeclaredMethods()) {
        if (isWriteEndpoint(method) && method.getAnnotation(Audited.class) == null) {
          missingAudits.add(controllerClass.getSimpleName() + "#" + method.getName());
        }
      }
    }

    assertThat(missingAudits).isEmpty();
  }

  private boolean isWriteEndpoint(Method method) {
    return method.getAnnotation(PostMapping.class) != null
        || method.getAnnotation(PatchMapping.class) != null
        || method.getAnnotation(DeleteMapping.class) != null;
  }
}
