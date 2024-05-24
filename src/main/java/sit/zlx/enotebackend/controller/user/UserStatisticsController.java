package sit.zlx.enotebackend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.service.*;
import sit.zlx.enotebackend.service.MyUtils.UsageBody;

import java.util.List;

import static sit.zlx.enotebackend.service.MyUtils.getUsageSize;

@RestController
@RequestMapping("/api/statistics")
public class UserStatisticsController {
    private final UserService userService;
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final FileService fileService;
    private final MongoTemplate mongoTemplate;


    @Autowired
    public UserStatisticsController(NoteService noteService, FolderService folderService, TagService tagService, FileService fileService, UserService userService, MongoTemplate mongoTemplate) {
        this.userService = userService;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.fileService = fileService;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/count")
    public ResponseDTO<CountBody> count(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            CountBody countBody = new CountBody();
            countBody.setNote(noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("userId", user.getId()).eq("isDeleting", 0)));
            countBody.setFolder(folderService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Folder>().eq("userId", user.getId()).eq("isDeleting", 0)));
            countBody.setTag(tagService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Tag>().eq("userId", user.getId())));
            countBody.setStared(noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("userId", user.getId()).eq("stared", 1)));
            countBody.setImage(fileService.count(new QueryWrapper<sit.zlx.enotebackend.domain.File>().eq("userId", user.getId()).eq("type", "image")));
            countBody.setVideo(fileService.count(new QueryWrapper<sit.zlx.enotebackend.domain.File>().eq("userId", user.getId()).eq("type", "video")));
            countBody.setAudio(fileService.count(new QueryWrapper<sit.zlx.enotebackend.domain.File>().eq("userId", user.getId()).eq("type", "audio")));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取成功！", countBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取失败！", null));
        }
    }

    @GetMapping("/usage")
    public ResponseDTO<UsageBody> usage(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            UsageBody.Size totalSize = new UsageBody.Size();

            List<String> noteIds = noteService.list(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("userId", user.getId()).select("id")).stream().map(sit.zlx.enotebackend.domain.Note::getId).toList();

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("_id").in(noteIds)),
                    Aggregation.project().andExclude("_id")
                            .andExpression("{ $bsonSize: '$$ROOT' }").as("tempSize"),
                    Aggregation.group().sum("tempSize").as("size")
            );

            AggregationResults<DocumentSize> results = mongoTemplate.aggregate(aggregation, "note", DocumentSize.class);
            DocumentSize documentTotalSize = results.getUniqueMappedResult();

            long noteTotalSize = 0L;
            if (documentTotalSize != null) {
                noteTotalSize = documentTotalSize.getSize();
            }
            String noteParsedTotalSize = MyUtils.File.convertRawSize(noteTotalSize);

            UsageBody.Size noteSize = getUsageSize(totalSize, List.of(noteTotalSize), noteParsedTotalSize);

            List<Long> imageSizes = fileService.list(new QueryWrapper<File>().eq("type", "image").eq("userId", user.getId()).select("size")).stream().map(File::getSize).toList();
            List<Long> videoSizes = fileService.list(new QueryWrapper<File>().eq("type", "video").eq("userId", user.getId()).select("size")).stream().map(File::getSize).toList();
            List<Long> audioSizes = fileService.list(new QueryWrapper<File>().eq("type", "audio").eq("userId", user.getId()).select("size")).stream().map(File::getSize).toList();
            String imageParsedTotalSize = MyUtils.File.sumItemSize(imageSizes);
            String videoParsedTotalSize = MyUtils.File.sumItemSize(videoSizes);
            String audioParsedTotalSize = MyUtils.File.sumItemSize(audioSizes);

            UsageBody.Size imageSize = getUsageSize(totalSize, imageSizes, imageParsedTotalSize);
            UsageBody.Size videoSize = getUsageSize(totalSize, videoSizes, videoParsedTotalSize);
            UsageBody.Size audioSize = getUsageSize(totalSize, audioSizes, audioParsedTotalSize);

            totalSize.setParsedSize(MyUtils.File.convertRawSize(totalSize.getRawSize()));
            UsageBody usageBody = new UsageBody(totalSize, noteSize, imageSize, videoSize, audioSize);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取使用情况成功！", usageBody));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取使用情况失败！", null));
        }
    }

    @Data
    @NoArgsConstructor
    public static class CountBody {
        private Long note;
        private Long folder;
        private Long tag;
        private Long stared;
        private Long image;
        private Long video;
        private Long audio;
    }

    @Data
    public static class DocumentSize {
        private long size;
    }
}
