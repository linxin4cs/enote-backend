package sit.zlx.enotebackend.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sit.zlx.enotebackend.domain.Folder;
import sit.zlx.enotebackend.domain.Note;
import sit.zlx.enotebackend.service.FolderService;
import sit.zlx.enotebackend.service.NoteService;
import java.util.Date;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class FolderDTO {
    private String id;
    private String name;
    private String parentId;
    private Date updatedAt;
    private Long noteCount;
    private Long subFolderCount;

    public static FolderDTO toDTO(Folder folder, NoteService noteService, FolderService folderService) {

        return FolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParentId())
                .updatedAt(folder.getUpdatedAt())
                .noteCount(noteService.count(new QueryWrapper<Note>().eq("folderId", folder.getId()).eq("isDeleting", 0)))
                .subFolderCount(folderService.count(new QueryWrapper<Folder>().eq("parentId", folder.getId()).eq("isDeleting", 0)))
                .build();
    }
}
