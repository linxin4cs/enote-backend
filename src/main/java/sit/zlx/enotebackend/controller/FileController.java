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
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    @Autowired
    FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> serveImage(@PathVariable String id) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("id", id));
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
    public ResponseEntity<Resource> serveVideo(@PathVariable String id) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("id", id));
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
    public ResponseEntity<Resource> serveAudio(@PathVariable String id) {
        try {
            File file = fileService.getOne(new QueryWrapper<File>().eq("id", id));
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
