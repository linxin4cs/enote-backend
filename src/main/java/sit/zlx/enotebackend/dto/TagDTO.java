package sit.zlx.enotebackend.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sit.zlx.enotebackend.domain.NoteTag;
import sit.zlx.enotebackend.domain.Tag;
import sit.zlx.enotebackend.service.NoteTagService;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class TagDTO {
    private String id;
    private String name;
    private Long noteCount;


    public static TagDTO toDTO(Tag tag, NoteTagService noteTagService) {
        return TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .noteCount(noteTagService.count(new QueryWrapper<NoteTag>().eq("tagId", tag.getId())))
                .build();
    }
}
