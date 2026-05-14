package com.njxs.lims.form.service;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.form.domain.FormSchemaRecord;
import com.njxs.lims.form.persistence.FormSchemaRepository;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FormSchemaService {

  private final FormSchemaRepository formSchemaRepository;

  public FormSchemaService(FormSchemaRepository formSchemaRepository) {
    this.formSchemaRepository = formSchemaRepository;
  }

  public List<FormSchemaRecord> list(String formCode, String status) {
    return formSchemaRepository.find(formCode, status);
  }

  public FormSchemaRecord current(String formCode, int schemaVersion) {
    return formSchemaRepository.find(formCode, "PUBLISHED").stream()
        .filter(schema -> schema.versionNo() == schemaVersion)
        .findFirst()
        .orElseThrow(() -> new BusinessException(
            "FORM_SCHEMA_NOT_FOUND",
            "表单版本不存在或未发布",
            HttpStatus.NOT_FOUND,
            Map.of("formCode", formCode, "schemaVersion", schemaVersion)
        ));
  }
}
