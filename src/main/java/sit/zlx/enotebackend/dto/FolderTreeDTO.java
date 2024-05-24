package sit.zlx.enotebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sit.zlx.enotebackend.domain.Folder;

import java.util.List;

@Data
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class FolderTreeDTO {
    private String value;
    private String label;
    private FolderTreeDTO[] children;

    public static FolderTreeDTO toDTO(Folder folder, List<Folder> folders) {
        List<Folder> children = new java.util.ArrayList<>(List.of());

        for (Folder f : folders) {
            if (f.getParentId() == null) {
                continue;
            }

            if (f.getParentId().equals(folder.getId())) {
                children.add(f);
            }
        }

        FolderTreeDTO[] childrenDTO = new FolderTreeDTO[children.size()];
        for (int i = 0; i < children.size(); i++) {
            childrenDTO[i] = toDTO(children.get(i), folders);
        }

        return FolderTreeDTO.builder()
                .value(folder.getId())
                .label(folder.getName())
                .children(childrenDTO)
                .build();
    }
}
