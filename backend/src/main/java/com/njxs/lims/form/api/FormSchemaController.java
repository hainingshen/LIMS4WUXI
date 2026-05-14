package com.njxs.lims.form.api;

import com.njxs.lims.form.service.FormSchemaService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FormSchemaController {

  private final FormSchemaService formSchemaService;

  public FormSchemaController(FormSchemaService formSchemaService) {
    this.formSchemaService = formSchemaService;
  }

  @GetMapping("/form-schemas")
  public List<FormSchemaResponse> list(
      @RequestParam(required = false) String formCode,
      @RequestParam(required = false) String status
  ) {
    return formSchemaService.list(formCode, status).stream()
        .map(FormSchemaResponse::from)
        .toList();
  }
}
