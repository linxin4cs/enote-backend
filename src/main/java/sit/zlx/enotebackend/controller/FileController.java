package sit.zlx.enotebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.service.FileService;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    @Autowired
    FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/image/{uuid}")
    public ResponseEntity<Resource> serveImage(@PathVariable String uuid) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("uuid", uuid));
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

    @GetMapping("/video/{uuid}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String uuid) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("uuid", uuid));
            Path videoPath = Path.of(file.getPath());
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

    @GetMapping("/audio/{uuid}")
    public ResponseEntity<Resource> serveAudio(@PathVariable String uuid) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("uuid", uuid));
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
}
