package com.njxs.lims.form.service;

import com.njxs.lims.form.domain.FormSchemaRecord;
import com.njxs.lims.form.persistence.FormSchemaRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
}
