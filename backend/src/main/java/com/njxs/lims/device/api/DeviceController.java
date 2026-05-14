package com.njxs.lims.device.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.device.service.DeviceService;
import com.njxs.lims.permission.RequiresAction;
import jakarta.validation.Valid;
import java.util.List;
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
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceController {

  private final DeviceService deviceService;

  public DeviceController(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @GetMapping("/devices")
  @RequiresAction("DEVICE_FILE_READ")
  public PageResponse<DeviceResponse> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return deviceService.list(keyword, page, pageSize);
  }

  @GetMapping("/devices/{id}/files")
  @RequiresAction("DEVICE_FILE_READ")
  public List<DeviceFileResponse> listFiles(@PathVariable UUID id) {
    return deviceService.listFiles(id);
  }

  @PostMapping("/devices/{id}/files")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "DEVICE_FILE_UPLOAD", entityType = "device_file")
  @RequiresAction("DEVICE_FILE_UPLOAD")
  public DeviceFileResponse uploadFile(
      @PathVariable UUID id,
      @Valid @RequestBody DeviceFileUploadRequest request
  ) {
    return deviceService.uploadFile(id, request, AuthContext.require());
  }

  @PostMapping("/device-files/{id}/disable")
  @Audited(actionCode = "DEVICE_FILE_DISABLE", entityType = "device_file")
  @RequiresAction("DEVICE_FILE_DISABLE")
  public DeviceFileResponse disableFile(@PathVariable UUID id) {
    return deviceService.disableFile(id, AuthContext.require());
  }
}
