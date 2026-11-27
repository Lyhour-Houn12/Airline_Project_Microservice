package com.jing.ancillary.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jing.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


@Component
@Slf4j
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements ImageStorageService {
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    @Override
    public StoredImage upload(MultipartFile file, String folder) {
        validate(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", false));
            return new StoredImage(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"));
        } catch (IOException ex) {
            throw new ImageStorageException("Failed to upload image", ex);
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException ex) {
            // best effort: an orphaned image is not worth failing the request
            log.warn("Failed to delete image from Cloudinary, publicId={}", publicId, ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Image must not exceed 5MB");
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase())) {
            throw new BadRequestException("Only JPEG, PNG or WEBP images are allowed");
        }
    }
}
