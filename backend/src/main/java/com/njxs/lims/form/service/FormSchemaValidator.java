package com.njxs.lims.form.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.njxs.lims.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FormSchemaValidator {

  public void validate(JsonNode schemaJson, JsonNode payload) {
    if (payload == null || !payload.isObject()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("missingFields", requiredFields(schemaJson))
      );
    }
    List<String> missingFields = requiredFields(schemaJson).stream()
        .filter(field -> isMissing(payload.get(field)))
        .toList();
    if (!missingFields.isEmpty()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("missingFields", missingFields)
      );
    }
  }

  private List<String> requiredFields(JsonNode schemaJson) {
    List<String> fields = new ArrayList<>();
    JsonNode required = schemaJson.path("required");
    if (!required.isArray()) {
      return fields;
    }
    required.forEach(field -> fields.add(field.asText()));
    return fields;
  }

  private boolean isMissing(JsonNode value) {
    return value == null
        || value.isNull()
        || (value.isTextual() && value.asText().isBlank());
  }
}
