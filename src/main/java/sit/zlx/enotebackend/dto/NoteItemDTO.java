package sit.zlx.enotebackend.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.*;
import sit.zlx.enotebackend.domain.*;
import sit.zlx.enotebackend.repository.NoteDocRepository;
import sit.zlx.enotebackend.service.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class NoteItemDTO {
    private String id;
    private String title;
    private String modifiedTime;
    private String abstractContent;
    private FolderInfo folderInfo;
    private int stared;
    private Long wordCount;
    private FileCount fileCount;
    private List<Tag> tags;

    public static NoteItemDTO toListItemDTO(Note note, ActiveNoteService activeNoteService, FolderService folderService, NoteDocRepository noteDocRepository) {
        Optional<NoteDoc> noteDoc = noteDocRepository.findById(note.getId());
        String abstractContent = "";
        if (noteDoc.isPresent()) {
            abstractContent = noteDoc.get().getTextContent().substring(0, Math.min(noteDoc.get().getTextContent().length(), 140));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置时区为东八区

        String modifiedTimeStr = sdf.format(activeNoteService.list(new QueryWrapper<ActiveNote>().eq("noteId", note.getId()).orderByDesc("modifiedTime")).get(0).getModifiedTime());

        Folder folder = folderService.getById(note.getFolderId());
        FolderInfo folderInfo = new FolderInfo();

        if (folder == null) {
            folderInfo.setFolderId("root");
            folderInfo.setFolderName("我的文件夹");
        } else {

            folderInfo.setFolderId(folder.getId());
            folderInfo.setFolderName(folder.getName());
        }


        return NoteItemDTO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .abstractContent(abstractContent)
                .modifiedTime(modifiedTimeStr)
                .folderInfo(folderInfo)
                .stared(note.getStared())
                .build();
    }


    public static NoteItemDTO toMoreInfoDTO(Note note, ActiveNoteService activeNoteService, FileService fileService, NoteDocRepository noteDocRepository, TagService tagService, NoteTagService noteTagService, FolderService folderService) {
        FileCount fileCount = new FileCount();
        fileCount.setImageCount(fileService.count(new QueryWrapper<File>().eq("noteId", note.getId()).eq("type", "image")));
        fileCount.setVideoCount(fileService.count(new QueryWrapper<File>().eq("noteId", note.getId()).eq("type", "video")));
        fileCount.setAudioCount(fileService.count(new QueryWrapper<File>().eq("noteId", note.getId()).eq("type", "audio")));

        Optional<NoteDoc> noteDoc = noteDocRepository.findById(note.getId());
        long wordCount = 0;
        String abstractContent = "";
        if (noteDoc.isPresent()) {
            wordCount = (MyUtils.calculateWordCount(noteDoc.get().getTextContent()));
            abstractContent = noteDoc.get().getTextContent().substring(0, Math.min(noteDoc.get().getTextContent().length(), 140));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置时区为东八区
        String modifiedTimeStr = sdf.format(activeNoteService.list(new QueryWrapper<ActiveNote>().eq("noteId", note.getId()).orderByDesc("modifiedTime")).get(0).getModifiedTime());

        // 先用noteTagService获取note的tagIds列表，再用tagService获取tagIds列表中的tag的详细信息
        List<String> tagIds = noteTagService.list(new QueryWrapper<NoteTag>().eq("noteId", note.getId())).stream().map(NoteTag::getTagId).collect(ArrayList::new, List::add, List::addAll);

        List<sit.zlx.enotebackend.domain.Tag> rawTags = new ArrayList< >();
        if(!tagIds.isEmpty()){
            rawTags = tagService.listByIds(tagIds);
        }
        List<Tag> tags = rawTags.stream().map(Tag::toTag).collect(ArrayList::new, List::add, List::addAll);

        Folder folder = folderService.getById(note.getFolderId());
        FolderInfo folderInfo = new FolderInfo();

        if (folder == null) {
            folderInfo.setFolderId("root");
            folderInfo.setFolderName("我的文件夹");
        } else {

            folderInfo.setFolderId(folder.getId());
            folderInfo.setFolderName(folder.getName());
        }

        return NoteItemDTO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .abstractContent(abstractContent)
                .modifiedTime(modifiedTimeStr)
                .folderInfo(folderInfo)
                .wordCount(wordCount)
                .stared(note.getStared())
                .fileCount(fileCount)
                .tags(tags)
                .build();
    }
}

@Data
@NoArgsConstructor
class FileCount {
    private Long imageCount;
    private Long videoCount;
    private Long audioCount;
}

@Data
@NoArgsConstructor
class FolderInfo {
    private String folderId;
    private String folderName;
}


@Data
@AllArgsConstructor
class Tag {
    private String id;
    private String name;


    public static Tag toTag(sit.zlx.enotebackend.domain.Tag tag) {
        return new Tag(tag.getId(), tag.getName());
    }
}
