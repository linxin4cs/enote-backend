package sit.zlx.enotebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.service.FileService;
import sit.zlx.enotebackend.service.UploadService;
import sit.zlx.enotebackend.service.UserService;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;
    private final UserService userService;
    private final UploadService uploadService;

    @Autowired
    FileController(FileService fileService, UserService userService, UploadService uploadService) {
        this.fileService = fileService;
        this.userService = userService;
        this.uploadService = uploadService;
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> serveImage(@AuthenticationPrincipal UserDetails currentUser, @PathVariable String id) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            File file = fileService.getOne(
                    new QueryWrapper<File>()
                            .eq("id", id)
                            .eq("userId", user.getId())
                            .eq("type", "image")
            );
            Path imagePath = Path.of(file.getPath());
            Resource resource = new UrlResource(imagePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
            } else {
                // 处理文件不存在的情况
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // 处理其他异常情况
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<Resource> serveVideo(@AuthenticationPrincipal UserDetails currentUser, @PathVariable String id) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            File file = fileService.getOne(new QueryWrapper<File>().eq("id", id).eq("userId", user.getId()).eq("type", "video"));
            Path videoPath = Paths.get(file.getPath());
            Resource resource = new UrlResource(videoPath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
            } else {
                // 处理文件不存在的情况
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // 处理其他异常情况
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<Resource> serveAudio(@AuthenticationPrincipal UserDetails currentUser, @PathVariable String id) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            File file = fileService.getOne(new QueryWrapper<File>().eq("id", id).eq("userId", user.getId()).eq("type", "audio"));
            Path AudioPath = Path.of(file.getPath());
            Resource resource = new UrlResource(AudioPath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
            } else {
                // 处理文件不存在的情况
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // 处理其他异常情况
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/image/{noteId}")
    public WangEditorUploadResponse uploadImage(@AuthenticationPrincipal UserDetails currentUser, @RequestParam("wangeditor-uploaded-image") MultipartFile[] files, @PathVariable String noteId) {
        if (files == null || files.length == 0) {
            return new WangEditorUploadResponse(1, "请上传文件", null);
        }

        if(files.length > 1) {
            return new WangEditorUploadResponse(1, "一次只能上传一个图片", null);
        }

        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            File file = uploadService.storeFile(files[0], UploadService.FILE_TYPE.IMAGE, user.getId(), noteId);
            String imageURL = "/api/file/image/" + file.getId();

            return new WangEditorUploadResponse(0, "上传成功", new WangEditorUploadResponse.DataResponse(imageURL));
        } catch (Exception e) {
            return new WangEditorUploadResponse(1, "上传失败", null);
        }
    }

    @PostMapping("/video/{noteId}")
    public WangEditorUploadResponse uploadVideo(@AuthenticationPrincipal UserDetails currentUser, @RequestParam("wangeditor-uploaded-video") MultipartFile[] files, @PathVariable String noteId) {
        if (files == null || files.length == 0) {
            return new WangEditorUploadResponse(1, "请上传文件", null);
        }

        if(files.length > 1) {
            return new WangEditorUploadResponse(1, "一次只能上传一个视频", null);
        }

        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            if(uploadService.isAudio(files[0].getOriginalFilename())) {
                File file = uploadService.storeFile(files[0], UploadService.FILE_TYPE.AUDIO, user.getId(), noteId);
                String audioURL = "/api/file/audio/" + file.getId();
                return new WangEditorUploadResponse(0, "上传成功", new WangEditorUploadResponse.DataResponse(audioURL));
            }

            File file = uploadService.storeFile(files[0], UploadService.FILE_TYPE.VIDEO, user.getId(), noteId);
            String videoURL = "/api/file/video/" + file.getId();

            return new WangEditorUploadResponse(0, "上传成功", new WangEditorUploadResponse.DataResponse(videoURL));
        } catch (Exception e) {
            return new WangEditorUploadResponse(1, "上传失败", null);
        }
    }

    @Data
    @AllArgsConstructor
    public static class UploadImageSuccessResponse {
        private String url;
    }

    @Data
    @AllArgsConstructor
    public static class UploadImageFailedResponse {
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class WangEditorUploadResponse {
        private int errno;
        private String message;
        private DataResponse data;

        @Data
        @AllArgsConstructor
        public static class DataResponse {
            private String url;
        }
    }
}
