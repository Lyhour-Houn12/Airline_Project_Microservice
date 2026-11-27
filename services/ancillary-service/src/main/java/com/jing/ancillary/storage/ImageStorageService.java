package com.jing.ancillary.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage upload(MultipartFile file, String folder);

    void delete(String publicId);
}
