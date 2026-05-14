package com.njxs.lims.report.registry.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.importexport.api.ImportExportTaskResponse;
import com.njxs.lims.importexport.service.ImportExportTaskService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.report.registry.service.ReportRegistryQuery;
import com.njxs.lims.report.registry.service.ReportRegistryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report-registry")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportRegistryController {

  private final ReportRegistryService reportRegistryService;
  private final ImportExportTaskService importExportTaskService;

  public ReportRegistryController(
      ReportRegistryService reportRegistryService,
      ImportExportTaskService importExportTaskService
  ) {
    this.reportRegistryService = reportRegistryService;
    this.importExportTaskService = importExportTaskService;
  }

  @GetMapping("/search")
  @RequiresAction("REPORT_REGISTRY_READ")
  public PageResponse<ReportRegistryItemResponse> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) UUID customerOrgId,
      @RequestParam(required = false) UUID projectId,
      @RequestParam(required = false) String reportBizNo,
      @RequestParam(required = false) String templateType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant issuedFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant issuedTo,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return reportRegistryService.search(
        new ReportRegistryQuery(
            keyword,
            customerOrgId,
            projectId,
            reportBizNo,
            templateType,
            status,
            issuedFrom,
            issuedTo
        ),
        page,
        pageSize
    );
  }

  @PostMapping("/batch-download")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Audited(actionCode = "REPORT_REGISTRY_BATCH_DOWNLOAD", entityType = "import_export_task")
  @RequiresAction("REPORT_REGISTRY_BATCH_DOWNLOAD")
  public ImportExportTaskResponse batchDownload(
      @Valid @RequestBody ReportRegistryBatchDownloadRequest request
  ) {
    return ImportExportTaskResponse.from(importExportTaskService.createReportBatchDownload(
        request.reportIds(),
        request.includeDeliveryLogOrDefault(),
        AuthContext.require()
    ));
  }
}
