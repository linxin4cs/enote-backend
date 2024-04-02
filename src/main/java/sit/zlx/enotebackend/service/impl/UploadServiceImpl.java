package sit.zlx.enotebackend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.service.FileService;
import sit.zlx.enotebackend.service.UploadService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class UploadServiceImpl implements UploadService {

    private final FileService fileService;
    @Value("${file.upload.base-dir}")
    private String uploadBaseDir;

    @Autowired
    UploadServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }


    @Override
    public File storeFile(MultipartFile multipartFile, FILE_TYPE fileType, int userId) throws Exception {
        String contentType = multipartFile.getContentType();
        if (isDisallowedContentType(contentType, fileType)) {
            throw new Exception("不允许上传的文件类型");
        }

        long fileSize = multipartFile.getSize();

        String uploadTargetDir = switch (fileType) {
            case IMAGE -> {
                if (fileSize > 1024 * 1024 * 4) {
                    throw new Exception("图片大小不能超过 4 MB");
                }

                yield  "image";
            }
            case VIDEO -> {
                if (fileSize > 1024 * 1024 * 512) {
                    throw new Exception("视频大小不能超过 512 MB");
                }

                yield "video";
            }
            case AUDIO -> {
                if (fileSize > 1024 * 1024 * 50) {
                    throw new Exception("音频大小不能超过 50 MB");
                }

                yield "audio";
            }
        };


        String storeFileName = generateFileName(multipartFile.getOriginalFilename());
        Path finalStorageLocation = Path.of(uploadBaseDir, uploadTargetDir);
        if (!Files.exists(finalStorageLocation))
            Files.createDirectories(finalStorageLocation);
        Path targetLocation = finalStorageLocation.resolve(storeFileName);
        Files.copy(multipartFile.getInputStream(), targetLocation);

        File file = new File();
        file.setUuid(storeFileName.replace("." + getExtension(storeFileName), ""));
        file.setUserId(userId);
        file.setType(fileType.toString());
        file.setName(multipartFile.getOriginalFilename());
        file.setPath(targetLocation.toString());
        file.setMimeType(contentType);
        file.setSize(fileSize);
        fileService.save(file);

        return file;
    }

    @Override
    public boolean isDisallowedContentType(String contentType, FILE_TYPE fileType) {
        Set<String> allowedContentTypes = switch (fileType) {
            case IMAGE -> new HashSet<>(Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp"));
            case VIDEO ->
                    new HashSet<>(Arrays.asList("video/mp4", "video/avi", "video/mkv", "video/webm", "video/mpeg"));
            case AUDIO -> new HashSet<>(Arrays.asList("audio/mpeg", "audio/mp3", "audio/wav", "audio/m4a"));
        };


        return !allowedContentTypes.contains(contentType);
    }

    private String generateFileName(String originalFilename) {
        String extension = getExtension(originalFilename);
        return UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
    }

    private String getExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1))
                .orElse("");
    }
}
