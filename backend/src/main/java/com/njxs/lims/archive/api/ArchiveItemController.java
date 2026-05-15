package com.njxs.lims.archive.api;

import com.njxs.lims.archive.service.ArchiveItemService;
import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/archive-items")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveItemController {

  private final ArchiveItemService archiveItemService;

  public ArchiveItemController(ArchiveItemService archiveItemService) {
    this.archiveItemService = archiveItemService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("ARCHIVE_READ")
  public PageResponse<ArchiveItemResponse> list(
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return archiveItemService.list(projectId, page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "ARCHIVE_MANAGE", entityType = "archive_item")
  @RequiresAction("ARCHIVE_MANAGE")
  public ArchiveItemResponse create(@Valid @RequestBody ArchiveItemCreateRequest request) {
    return archiveItemService.create(request, AuthContext.require());
  }
}
