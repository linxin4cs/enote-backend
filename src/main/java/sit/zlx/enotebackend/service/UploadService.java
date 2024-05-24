package sit.zlx.enotebackend.service;

import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;

public interface UploadService {
    File storeFile(MultipartFile file, FILE_TYPE fileType, String userId, String noteId) throws Exception;

    // 根据后缀名判断是否为音频文件，忽略大小写，支持 mp3, wav, m4a以及mpeg格式
    boolean isAudio(String fileName);

    boolean isDisallowedContentType(String contentType, FILE_TYPE fileType);

    enum FILE_TYPE {
        IMAGE,
        VIDEO,
        AUDIO,
    }
}
