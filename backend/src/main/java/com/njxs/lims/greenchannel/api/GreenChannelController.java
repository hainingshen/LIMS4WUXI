package com.njxs.lims.greenchannel.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.greenchannel.service.GreenChannelService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/green-channel-requests")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreenChannelController {

  private final GreenChannelService greenChannelService;

  public GreenChannelController(GreenChannelService greenChannelService) {
    this.greenChannelService = greenChannelService;
  }

  @GetMapping
  @RequiresClient(ClientType.INTERNAL)
  public PageResponse<GreenChannelResponse> list(
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return greenChannelService.list(projectId, page, pageSize);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "GREEN_CHANNEL_CREATE", entityType = "green_channel_request")
  @RequiresAction("GREEN_CHANNEL_CREATE")
  public GreenChannelResponse create(@Valid @RequestBody GreenChannelCreateRequest request) {
    return greenChannelService.create(request, AuthContext.require());
  }

  @PostMapping("/{id}/approve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Audited(actionCode = "GREEN_CHANNEL_APPROVE", entityType = "green_channel_request")
  public void approve(
      @PathVariable UUID id,
      @Valid @RequestBody GreenChannelApprovalRequest request
  ) {
    greenChannelService.approve(id, request, AuthContext.require());
  }
}
