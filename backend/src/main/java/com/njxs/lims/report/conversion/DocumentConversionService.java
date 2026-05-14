package com.njxs.lims.report.conversion;

import com.njxs.lims.file.domain.StoredFile;
import java.util.UUID;

public interface DocumentConversionService {

  StoredFile convertDocxToPdf(UUID docxFileId);
}
