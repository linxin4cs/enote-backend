package sit.zlx.enotebackend.service;

import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;

import java.io.IOException;

public interface UploadService {
    File storeFile(MultipartFile file, FILE_TYPE fileType, int userId) throws Exception;

    boolean isDisallowedContentType(String contentType, FILE_TYPE fileType);

    enum FILE_TYPE {
        IMAGE,
        VIDEO,
        AUDIO,
    }
}
