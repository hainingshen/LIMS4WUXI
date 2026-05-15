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
      String contentType = validatedContentType(file, bytes);
      String fileName = fileName(file);
      String objectKey = folder + "/" + UUID.randomUUID() + "-" + fileName;
      ensureBucket();
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(properties.getBucket())
          .object(objectKey)
          .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
          .contentType(contentType)
          .build());
      return new StoredFile(
          UUID.randomUUID(),
          fileName,
          objectKey,
          contentType,
          sha256(bytes),
          bytes.length
      );
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(
          "FILE_STORAGE_FAILED",
          "文件存储失败",
          HttpStatus.BAD_GATEWAY,
          Map.of("reason", exception.getClass().getSimpleName())
      );
    }
  }

  private String validatedContentType(MultipartFile file, byte[] bytes) {
    if (bytes.length == 0) {
      throw new BusinessException(
          "FILE_EMPTY",
          "文件不能为空",
          HttpStatus.BAD_REQUEST,
          Map.of()
      );
    }
    String contentType = normalizeContentType(file.getContentType());
    if (contentType == null) {
      throw unsupported(file.getContentType());
    }
    boolean matches = switch (contentType) {
      case "image/jpeg" -> isJpeg(bytes);
      case "image/png" -> isPng(bytes);
      case "application/pdf" -> isPdf(bytes);
      case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> isZip(bytes);
      default -> throw unsupported(contentType);
    };
    if (!matches) {
      throw new BusinessException(
          "FILE_CONTENT_MISMATCH",
          "文件内容与声明类型不匹配",
          HttpStatus.BAD_REQUEST,
          Map.of("contentType", contentType, "fileName", fileName(file))
      );
    }
    return contentType;
  }

  private BusinessException unsupported(String contentType) {
    return new BusinessException(
        "FILE_TYPE_UNSUPPORTED",
        "不支持的文件类型",
        HttpStatus.BAD_REQUEST,
        Map.of("contentType", String.valueOf(contentType))
    );
  }

  private String normalizeContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return null;
    }
    return contentType.split(";", 2)[0].trim().toLowerCase();
  }

  private boolean isJpeg(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xff) == 0xff
        && (bytes[1] & 0xff) == 0xd8
        && (bytes[2] & 0xff) == 0xff;
  }

  private boolean isPng(byte[] bytes) {
    byte[] signature = new byte[] {
        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    if (bytes.length < signature.length) {
      return false;
    }
    for (int index = 0; index < signature.length; index++) {
      if (bytes[index] != signature[index]) {
        return false;
      }
    }
    return true;
  }

  private boolean isPdf(byte[] bytes) {
    return bytes.length >= 5
        && bytes[0] == '%'
        && bytes[1] == 'P'
        && bytes[2] == 'D'
        && bytes[3] == 'F'
        && bytes[4] == '-';
  }

  private boolean isZip(byte[] bytes) {
    return bytes.length >= 4
        && bytes[0] == 'P'
        && bytes[1] == 'K'
        && (bytes[2] == 0x03 || bytes[2] == 0x05 || bytes[2] == 0x07)
        && (bytes[3] == 0x04 || bytes[3] == 0x06 || bytes[3] == 0x08);
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
