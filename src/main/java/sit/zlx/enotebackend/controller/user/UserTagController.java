package sit.zlx.enotebackend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.NoteTag;
import sit.zlx.enotebackend.domain.Tag;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.PaginatedDTO;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.dto.TagDTO;
import sit.zlx.enotebackend.service.NoteTagService;
import sit.zlx.enotebackend.service.SearchHistoryService;
import sit.zlx.enotebackend.service.TagService;
import sit.zlx.enotebackend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
public class UserTagController {
    private final UserService userService;
    private final NoteTagService noteTagService;
    private final TagService tagService;
    private final SearchHistoryService searchHistoryService;

    @Autowired
    public UserTagController(UserService userService, NoteTagService noteTagService, TagService tagService, SearchHistoryService searchHistoryService) {
        this.userService = userService;
        this.noteTagService = noteTagService;
        this.tagService = tagService;
        this.searchHistoryService = searchHistoryService;
    }


    private static QueryWrapper<Tag> getTagsQueryWrapper(String userId, ListTagBody.Query searchParams) {
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("name");

        queryWrapper.eq("userId", userId);


        // 根据搜索参数构建查询条件
        if (searchParams != null) {
            if (searchParams.getKeyword() != null) {
                if (searchParams.getIsCaseSensitive() == 0) {
                    queryWrapper.like("LOWER(name)", searchParams.getKeyword().toLowerCase());
                } else {
                    // 不区分大小写
                    queryWrapper.like("name", searchParams.getKeyword());
                }
            }
        }

        return queryWrapper;
    }

    @PostMapping("/list")
    public ResponseDTO<PaginatedDTO<TagDTO>> list(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<ListTagBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            ListTagBody pageParams = requestDTO.getData();
            ListTagBody.Query searchParams = pageParams.getQuery(); // 获取搜索参数
            int page = pageParams.getPage();
            int size = pageParams.getSize();

            Page<Tag> pageObj = new Page<>(page, size);

            QueryWrapper<Tag> queryWrapper = getTagsQueryWrapper(user.getId(), searchParams);

            IPage<Tag> tagPage = tagService.page(pageObj, queryWrapper);
            List<Tag> tags = tagPage.getRecords();

            if(searchParams != null && searchParams.getKeyword() != null && !searchParams.getKeyword().isEmpty()) {
                searchHistoryService.saveSearchHistory(user.getId(), searchParams.getKeyword().trim());
            }

            ResponseDTO.ResponseData<PaginatedDTO<TagDTO>> responseData = new ResponseDTO.ResponseData<>("获取标签列表成功！", new PaginatedDTO<>(tags.stream().map(tag -> TagDTO.toDTO(tag, noteTagService)).toList(), (int) tagPage.getCurrent(), (int) tagPage.getTotal(), (int) tagPage.getPages()));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), responseData);
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取标签列表失败！", null));
        }
    }

    @PostMapping("/rename")
    public ResponseDTO<?> rename(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<RenameTagBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            RenameTagBody body = requestDTO.getData();
            String tagId = body.getId();
            String tagName = body.getName();

            Tag tag = tagService.getOne(new QueryWrapper<Tag>().eq("id", tagId).eq("userId", user.getId()));
            if (tag == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.NOT_FOUND.getCode(), new ResponseDTO.ResponseData<>("标签不存在！", null));
            }

            // 检查是否有重名的标签

            Tag tagWithSameName = tagService.getOne(new QueryWrapper<Tag>().eq("name", tagName).eq("userId", user.getId()));
            if (tagWithSameName != null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("已存在同名标签！", null));
            }

            tag.setName(tagName);
            tagService.updateById(tag);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("重命名标签成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("重命名标签失败！", null));
        }
    }

    @PostMapping("/delete")
    public ResponseDTO<?> delete(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<DeleteTagBody> requestDTO) {
        // 需要先删除 noteTag 表中的记录，再删除 tag 表中的记录
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            DeleteTagBody body = requestDTO.getData();
            String tagId = body.getId();

            Tag tag = tagService.getOne(new QueryWrapper<Tag>().eq("id", tagId).eq("userId", user.getId()));
            if (tag == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.NOT_FOUND.getCode(), new ResponseDTO.ResponseData<>("标签不存在！", null));
            }

            noteTagService.remove(new QueryWrapper<NoteTag>().eq("tagId", tagId));
            tagService.removeById(tagId);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除标签成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除标签失败！", null));
        }
    }

    @PostMapping("/get-one")
    public ResponseDTO<TagDTO> getOne(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<TagIdBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            TagIdBody tagIdBody = requestDTO.getData();

            Tag tag = tagService.getOne(new QueryWrapper<Tag>().eq("id", tagIdBody.getId()).eq("userId", user.getId()));

            if (tag == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("标签不存在！", null));
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取当前标签信息成功！", TagDTO.toDTO(tag, noteTagService)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取当前标签信息失败！", null));
        }
    }

    @Data
    @NoArgsConstructor
    public static class ListTagBody {
        private int page;
        private int size;
        private Query query;

        @Data
        @NoArgsConstructor
        public static class Query {
            private String keyword;
            private int isCaseSensitive;
        }
    }

    @Data
    @NoArgsConstructor
    public static class RenameTagBody {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class DeleteTagBody {
        private String id;
    }

    @Data
    @NoArgsConstructor
    public static class TagIdBody {
        private String id;
    }
}
