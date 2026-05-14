package com.njxs.lims.report.conversion;

import com.njxs.lims.file.domain.StoredFile;
import com.njxs.lims.report.persistence.ReportRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlaceholderDocumentConversionService implements DocumentConversionService {

  private static final String PDF_CONTENT_TYPE = "application/pdf";

  private final ReportRepository reportRepository;

  public PlaceholderDocumentConversionService(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public StoredFile convertDocxToPdf(UUID docxFileId) {
    String marker = "pdf-placeholder:" + docxFileId;
    byte[] bytes = marker.getBytes(StandardCharsets.UTF_8);
    String fileName = "official-" + docxFileId + ".pdf";
    String objectKey = "reports/conversions/" + docxFileId + ".pdf";
    UUID fileId = reportRepository.saveGeneratedFileMetadata(
        fileName,
        objectKey,
        PDF_CONTENT_TYPE,
        bytes.length,
        sha256(bytes),
        null
    );
    return new StoredFile(fileId, fileName, objectKey, PDF_CONTENT_TYPE, bytes.length, sha256(bytes));
  }

  private String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder hex = new StringBuilder();
      for (byte value : digest) {
        hex.append("%02x".formatted(value));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
