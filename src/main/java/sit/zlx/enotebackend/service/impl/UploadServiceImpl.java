package sit.zlx.enotebackend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.NoteDoc;
import sit.zlx.enotebackend.repository.NoteDocRepository;
import sit.zlx.enotebackend.service.FileService;
import sit.zlx.enotebackend.service.UploadService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static sit.zlx.enotebackend.service.UploadService.FILE_TYPE.AUDIO;

@Service
public class UploadServiceImpl implements UploadService {

    private final FileService fileService;
    @Value("${file.upload.base-dir}")
    private String uploadBaseDir;
    private final NoteDocRepository noteDocRepository;

    @Autowired
    UploadServiceImpl(FileService fileService, NoteDocRepository noteDocRepository) {
        this.fileService = fileService;
        this.noteDocRepository = noteDocRepository;
    }

    @Override
    public File storeFile(MultipartFile multipartFile, FILE_TYPE fileType, String userId, String noteId) throws Exception {
        if (noteId == null || noteId.isEmpty())
            return processFile(multipartFile, fileType, userId, null);

        return processFile(multipartFile, fileType, userId, noteId);
    }

    public File processFile(MultipartFile multipartFile, FILE_TYPE fileType, String userId, String noteId) throws Exception {
        String contentType = multipartFile.getContentType();
        if (fileType != AUDIO && isDisallowedContentType(contentType, fileType)) {
            throw new Exception("不允许上传的文件类型");
        }

        if (fileType == AUDIO && !isAudio(multipartFile.getOriginalFilename())) {
            throw new Exception("不允许上传的文件类型");
        }

        long fileSize = multipartFile.getSize();

        String uploadTargetDir = switch (fileType) {
            case IMAGE -> {
                if (fileSize > 1024 * 1024 * 10) {
                    throw new Exception("图片大小不能超过 10 MB");
                }

                yield "image";
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
        file.setId(storeFileName.replace("." + getExtension(storeFileName), ""));
        file.setUserId(userId);
        file.setType(fileType.toString());
        file.setName(multipartFile.getOriginalFilename());
        file.setPath(targetLocation.toString());
        file.setMimeType(contentType);
        file.setSize(fileSize);
        file.setNoteId(noteId);
        fileService.save(file);

        if (noteId != null) {
            NoteDoc noteDoc = noteDocRepository.findById(noteId).orElseThrow();
            List<String> attachments = noteDoc.getAttachments();
            if (attachments == null) {
                attachments = new ArrayList<>();
            }

            attachments.add(file.getId());
            noteDoc.setAttachments(attachments);
            noteDocRepository.save(noteDoc);
        }

        return file;
    }

    @Override
    // 根据后缀名判断是否为音频文件，忽略大小写，支持 mp3, wav, m4a以及mpeg格式
    public boolean isAudio(String fileName) {
        String extension = getExtension(fileName);
        return extension.equalsIgnoreCase("mp3") || extension.equalsIgnoreCase("wav") || extension.equalsIgnoreCase("m4a") || extension.equalsIgnoreCase("mpeg");
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
