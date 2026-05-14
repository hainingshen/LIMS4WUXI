package com.njxs.lims.form.persistence;

import com.njxs.lims.form.domain.FormSchemaRecord;
import java.util.List;

public interface FormSchemaRepository {

  List<FormSchemaRecord> find(String formCode, String status);
}
