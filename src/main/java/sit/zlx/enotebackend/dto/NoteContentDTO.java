package sit.zlx.enotebackend.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.*;
import sit.zlx.enotebackend.domain.ActiveNote;
import sit.zlx.enotebackend.domain.Folder;
import sit.zlx.enotebackend.domain.Note;
import sit.zlx.enotebackend.domain.NoteDoc;
import sit.zlx.enotebackend.service.ActiveNoteService;
import sit.zlx.enotebackend.service.FolderService;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class NoteContentDTO {
    private String id;
    private String title;
    private String modifiedTime;
    private int stared;
    private String content;
    private String abstractContent;
    private FolderInfo folderInfo;

    @Data
    @NoArgsConstructor
    static class  FolderInfo {
        private String folderId;
        private String folderName;
    }


    public static NoteContentDTO toDTO(Note note, NoteDoc noteDoc, ActiveNoteService activeNoteService, FolderService folderService) {
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

        return NoteContentDTO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .modifiedTime(modifiedTimeStr)
                .stared(note.getStared())
                .content(noteDoc.getHtmlContent())
                .abstractContent(noteDoc.getTextContent().substring(0, Math.min(noteDoc.getTextContent().length(), 140)))
                .folderInfo(folderInfo)
                .build();
    }
}




