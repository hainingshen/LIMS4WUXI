package com.njxs.lims.file.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorage {

  StoredFile store(MultipartFile file, String folder);
}
