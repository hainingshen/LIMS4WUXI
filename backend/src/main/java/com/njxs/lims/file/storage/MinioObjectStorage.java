package com.njxs.lims.file.storage;

import com.njxs.lims.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "lims.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioObjectStorage implements ObjectStorage {

  private final StorageProperties properties;
  private final MinioClient minioClient;

  public MinioObjectStorage(StorageProperties properties) {
    this.properties = properties;
    this.minioClient = MinioClient.builder()
        .endpoint(properties.getEndpoint())
        .credentials(properties.getAccessKey(), properties.getSecretKey())
        .build();
  }

  @Override
  public StoredFile store(MultipartFile file, String folder) {
    try {
      byte[] bytes = file.getBytes();
      String fileName = fileName(file);
      String objectKey = folder + "/" + UUID.randomUUID() + "-" + fileName;
      ensureBucket();
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(properties.getBucket())
          .object(objectKey)
          .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
          .contentType(file.getContentType())
          .build());
      return new StoredFile(
          UUID.randomUUID(),
          fileName,
          objectKey,
          file.getContentType(),
          sha256(bytes),
          bytes.length
      );
    } catch (Exception exception) {
      throw new BusinessException(
          "FILE_STORAGE_FAILED",
          "文件存储失败",
          HttpStatus.BAD_GATEWAY,
          Map.of("reason", exception.getClass().getSimpleName())
      );
    }
  }

  private void ensureBucket() throws Exception {
    boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
        .bucket(properties.getBucket())
        .build());
    if (!exists) {
      minioClient.makeBucket(MakeBucketArgs.builder()
          .bucket(properties.getBucket())
          .build());
    }
  }

  private String fileName(MultipartFile file) {
    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      return "upload.bin";
    }
    return original.replace("\\", "_").replace("/", "_");
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
