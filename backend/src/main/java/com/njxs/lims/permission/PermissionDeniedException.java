package com.njxs.lims.permission;

import com.njxs.lims.common.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PermissionDeniedException extends BusinessException {

  public PermissionDeniedException(String actionCode) {
    super(
        "FORBIDDEN",
        "权限不足",
        HttpStatus.FORBIDDEN,
        Map.of("actionCode", actionCode)
    );
  }
}
