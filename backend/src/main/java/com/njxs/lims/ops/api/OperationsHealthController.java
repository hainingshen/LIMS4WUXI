package com.njxs.lims.ops.api;

import com.njxs.lims.common.web.TraceIdFilter;
import com.njxs.lims.file.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
public class OperationsHealthController {

  private final JdbcTemplate jdbcTemplate;
  private final MinioClient minioClient;
  private final StorageProperties storageProperties;

  public OperationsHealthController(JdbcTemplate jdbcTemplate, StorageProperties storageProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.storageProperties = storageProperties;
    this.minioClient = MinioClient.builder()
        .endpoint(storageProperties.getEndpoint())
        .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
        .build();
  }

  @GetMapping("/health")
  public ResponseEntity<OperationsHealthResponse> health(HttpServletRequest request) {
    Map<String, String> checks = new LinkedHashMap<>();
    checks.put("database", checkDatabase());
    checks.put("objectStorage", checkObjectStorage());
    boolean up = checks.values().stream().allMatch("UP"::equals);
    OperationsHealthResponse response = new OperationsHealthResponse(
        up ? "UP" : "DEGRADED",
        Instant.now(),
        traceId(request),
        checks
    );
    return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  private String checkDatabase() {
    try {
      Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
      return Integer.valueOf(1).equals(value) ? "UP" : "DOWN";
    } catch (RuntimeException exception) {
      return "DOWN";
    }
  }

  private String checkObjectStorage() {
    try {
      minioClient.bucketExists(BucketExistsArgs.builder()
          .bucket(storageProperties.getBucket())
          .build());
      return "UP";
    } catch (Exception exception) {
      return "DOWN";
    }
  }

  private String traceId(HttpServletRequest request) {
    Object value = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    return value instanceof String traceId && !traceId.isBlank() ? traceId : "missing-trace-id";
  }
}
