package com.njxs.lims.importexport.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import com.njxs.lims.importexport.service.ImportExportTaskService;
import com.njxs.lims.permission.RequiresAction;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import-export-tasks")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImportExportTaskController {

  private final ImportExportTaskService importExportTaskService;

  public ImportExportTaskController(ImportExportTaskService importExportTaskService) {
    this.importExportTaskService = importExportTaskService;
  }

  @GetMapping
  @RequiresAction("IMPORT_EXPORT_TASK_CREATE")
  public PageResponse<ImportExportTaskResponse> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return importExportTaskService.list(page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "IMPORT_EXPORT_TASK_CREATE", entityType = "import_export_task")
  @RequiresAction("IMPORT_EXPORT_TASK_CREATE")
  public ImportExportTaskResponse create(@Valid @RequestBody ImportExportTaskCreateRequest request) {
    return ImportExportTaskResponse.from(importExportTaskService.create(request, AuthContext.require()));
  }

  @PostMapping("/{id}/validate")
  @Audited(actionCode = "IMPORT_EXPORT_TASK_VALIDATE", entityType = "import_export_task")
  @RequiresAction("IMPORT_EXPORT_TASK_VALIDATE")
  public ImportExportTaskResponse validate(@PathVariable UUID id) {
    return ImportExportTaskResponse.from(importExportTaskService.validate(id, AuthContext.require()));
  }

  @PostMapping("/{id}/run")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Audited(actionCode = "IMPORT_EXPORT_TASK_RUN", entityType = "import_export_task")
  @RequiresAction("IMPORT_EXPORT_TASK_RUN")
  public ImportExportTaskResponse run(@PathVariable UUID id) {
    return ImportExportTaskResponse.from(importExportTaskService.run(id, AuthContext.require()));
  }

  @GetMapping(value = "/{id}/result-file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @RequiresAction("IMPORT_EXPORT_TASK_RESULT_DOWNLOAD")
  public ResponseEntity<byte[]> downloadResultFile(@PathVariable UUID id) {
    ImportExportTaskRecord task = importExportTaskService.resultReady(id);
    byte[] bytes = ("{\"taskId\":\"" + task.id() + "\",\"status\":\"" + task.status() + "\"}\n")
        .getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + task.bizNo() + "-result.json\"")
        .body(bytes);
  }
}
