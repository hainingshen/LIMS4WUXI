package com.njxs.lims.importexport.persistence;

import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportExportTaskRepository {

  String nextBizNo(int year);

  ImportExportTaskRecord save(ImportExportTaskRecord record, String parametersJson, UUID actorUserId);

  List<ImportExportTaskRecord> list(int limit, int offset);

  int count();

  Optional<ImportExportTaskRecord> findById(UUID id);

  ImportExportTaskRecord updateStatus(
      UUID id,
      String status,
      UUID resultFileId,
      String errorSummary,
      java.time.Instant expiresAt,
      UUID actorUserId
  );

  boolean existsFile(UUID fileId);

  UUID saveResultFileMetadata(
      String fileName,
      String objectKey,
      String contentType,
      long sizeBytes,
      String sha256,
      UUID actorUserId
  );
}
