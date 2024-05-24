package sit.zlx.enotebackend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import sit.zlx.enotebackend.domain.Folder;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.*;
import sit.zlx.enotebackend.service.*;

import java.util.*;

@RestController
@RequestMapping("/api/folder")
public class UserFolderController {
    private final UserService userService;
    private final NoteService noteService;
    private final FolderService folderService;
    private final UserFolderService userFolderService;
    private final SearchHistoryService searchHistoryService;

    @Autowired
    public UserFolderController(UserService userService, NoteService noteService, FolderService folderService, UserFolderService userFolderService, SearchHistoryService searchHistoryService) {
        this.userService = userService;
        this.noteService = noteService;
        this.folderService = folderService;
        this.userFolderService = userFolderService;
        this.searchHistoryService = searchHistoryService;
    }

    private QueryWrapper<Folder> getFoldersQueryWrapper(String userId, ListFolderBody.Query searchParams) {
        QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("updatedAt");

        queryWrapper.eq("userId", userId);

        // 先过滤掉正在删除的
        queryWrapper.eq("isDeleting", 0);

        // 根据搜索参数构建查询条件
        if (searchParams != null) {
            if (searchParams.getKeyword() != null && !Objects.equals(searchParams.getKeyword().trim(), "")) {
                if (searchParams.getIsCaseSensitive() == 0) {
                    queryWrapper.like("LOWER(name)", searchParams.getKeyword().trim().toLowerCase());
                } else {
                    queryWrapper.like("name", searchParams.getKeyword().trim());
                }
            }

            if (searchParams.getParentId() != null) {
                if (searchParams.getParentId().equals("root")) {
                    queryWrapper.isNull("parentId");
                } else {
                    queryWrapper.eq("parentId", searchParams.getParentId());
                }
            }
        }

        return queryWrapper;
    }

    @PostMapping("/list")
    public ResponseDTO<PaginatedDTO<FolderDTO>> list(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<ListFolderBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            ListFolderBody pageParams = requestDTO.getData();
            ListFolderBody.Query searchParams = pageParams.getQuery(); // 获取搜索参数
            int page = pageParams.getPage();
            int size = pageParams.getSize();

            Page<Folder> pageObj = new Page<>(page, size);

            QueryWrapper<Folder> queryWrapper = getFoldersQueryWrapper(user.getId(), searchParams);

            IPage<Folder> folderPage = folderService.page(pageObj, queryWrapper);
            List<Folder> folders = folderPage.getRecords();

            if (searchParams != null && searchParams.getKeyword() != null && !searchParams.getKeyword().trim().isEmpty()) {
                searchHistoryService.saveSearchHistory(user.getId(), searchParams.getKeyword().trim());
            }


            ResponseDTO.ResponseData<PaginatedDTO<FolderDTO>> responseData = new ResponseDTO.ResponseData<>("获取文件夹列表成功！", new PaginatedDTO<>(folders.stream().map(folder -> FolderDTO.toDTO(folder, noteService, folderService)).toList(), (int) folderPage.getCurrent(), (int) folderPage.getTotal(), (int) folderPage.getPages()));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), responseData);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取文件夹列表失败！", null));
        }
    }

    @PostMapping("/delete")
    public ResponseDTO<?> delete(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<DeleteFolderBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            String folderId = requestDTO.getData().getId();
            // 需要先删除子文件夹，因为子文件夹的 parentId 有外键约束
            // 删除文件夹需要先删除文件夹中的笔记，因为笔记的 folderId 有外键约束
            // 删除笔记需要先删除笔记的文件，因为文件的 noteId 有外键约束
            // 之后再删除笔记相关记录
            // 最后删除文件夹

            Folder folder = folderService.getOne(new QueryWrapper<Folder>().eq("id", folderId).eq("userId", userId));

            if (folder == null || folder.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("文件夹不存在！", null));
            }

            folderService.updateUpdatedAt(folderId);

            // 获取所有子文件夹的列表（包括当前文件夹）
            List<String> folderIds = userFolderService.getAllSubfolderIds(folderId);

            // 逆序删除文件夹和其内容
            Collections.reverse(folderIds); // 确保从最深层的子文件夹开始删除
            for (String id : folderIds) {
                folderService.update(new UpdateWrapper<Folder>().set("isDeleting", 1).eq("id", id));

                List<String> noteIds = noteService.list(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("folderId", id)).stream().map(sit.zlx.enotebackend.domain.Note::getId).toList();

                if (!noteIds.isEmpty()) {
                    noteService.update(new UpdateWrapper<sit.zlx.enotebackend.domain.Note>().set("isDeleting", 1).in("id", noteIds));

                }

                userFolderService.deleteFolderData(id, noteIds);
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除文件夹成功！", null));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除文件夹失败！", null));
        }
    }

    @PostMapping("/create")
    public ResponseDTO<FolderDTO> create(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<CreateFolderBody> requestDTO) {
        try {
            CreateFolderBody createFolderBody = requestDTO.getData();
            String parsedName = createFolderBody.getName().trim();
            String parentId = createFolderBody.getParentId();
            String parsedNameValidation = MyUtils.Validator.validateFolderName(parsedName);

            if (!parsedNameValidation.isEmpty()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(parsedNameValidation, null));
            }

            Folder searchFolder;

            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            if (parentId == null) {
                searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("userId", user.getId()));

            } else {
                if (parentId.equals("root")) {
                    searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("userId", user.getId()).isNull("parentId"));
                } else {
                    Folder parentFolder = folderService.getById(parentId);

                    if (parentFolder == null || parentFolder.getIsDeleting() == 1) {
                        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("父文件夹不存在！", null));
                    }

                    searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("parentId", parentId).eq("userId", user.getId()));
                }
            }

            if (searchFolder != null && searchFolder.getIsDeleting() == 0) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("目录中存在重名文件夹！", null));
            }

            Folder newFolder = new Folder();
            newFolder.setId(UUID.randomUUID().toString());
            if (!(parentId == null || parentId.equals("root"))) {
                newFolder.setParentId(parentId);
            }
            newFolder.setName(parsedName);
            newFolder.setUserId(user.getId());

            folderService.save(newFolder);
            folderService.updateUpdatedAt(newFolder.getId());

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("创建文件夹成功！", FolderDTO.toDTO(folderService.getById(newFolder.getId()), noteService, folderService)));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("创建文件夹失败！", null));
        }
    }

    @PostMapping("/update")
    public ResponseDTO<FolderDTO> update(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<UpdateFolderBody> requestDTO) {
        try {
            UpdateFolderBody updateFolderBody = requestDTO.getData();
            String parsedName = updateFolderBody.getName().trim();
            String parentId = updateFolderBody.getParentId();
            String parsedNameValidation = MyUtils.Validator.validateFolderName(parsedName);

            if (!parsedNameValidation.isEmpty()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(parsedNameValidation, null));
            }

            Folder searchFolder;
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            if (parentId == null) {
                searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("userId", userId));
            } else {
                if (parentId.equals("root")) {
                    searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("userId", userId).isNull("parentId"));
                } else {


                    if (parentId.equals(updateFolderBody.getId())) {
                        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("父文件夹不能是自己！", null));
                    }

                    Folder parentFolder = folderService.getById(parentId);

                    if (parentFolder == null || parentFolder.getIsDeleting() == 1) {
                        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("父文件夹不存在！", null));
                    }

                    searchFolder = folderService.getOne(new QueryWrapper<Folder>().eq("name", parsedName).eq("userId", userId).eq("parentId", parentId));
                }
            }

            if (searchFolder != null && searchFolder.getIsDeleting() == 0) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("目录中存在重名文件夹！", null));
            }

            Folder folder = folderService.getById(updateFolderBody.getId());

            if (folder == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("文件夹不存在！", null));
            }

            folder.setName(parsedName);

            if (parentId == null || parentId.equals("root")) {
                folder.setParentId(null);
            } else {
                folder.setParentId(parentId);
            }

            folderService.updateById(folder);
            folderService.updateUpdatedAt(folder.getId());

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("更新文件夹成功！", FolderDTO.toDTO(folder, noteService, folderService)));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("更新文件夹失败！", null));
        }
    }

    @PostMapping("/get-tree")
    public ResponseDTO<FolderTreeDTO> getTree(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<FolderIdBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            List<Folder> folders = folderService.list(new QueryWrapper<Folder>().eq("userId", user.getId()).eq("isDeleting", 0));

            // 按照文件夹名称排序
            folders.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            // 去掉当前文件夹
            folders.removeIf(folder -> folder.getId().equals(requestDTO.getData().getId()));

            FolderTreeDTO root = new FolderTreeDTO();
            root.setValue("root");
            root.setLabel("我的文件夹");
            root.setChildren(new FolderTreeDTO[0]);

            List<FolderTreeDTO> folderTreeDTOList = new java.util.ArrayList<>(folders.stream().map(folder -> {
                if (folder.getParentId() != null) {
                    return null;
                }

                return FolderTreeDTO.toDTO(folder, folders);
            }).toList());

            // 去掉 null 元素
            folderTreeDTOList.removeIf(Objects::isNull);

            root.setChildren(folderTreeDTOList.toArray(new FolderTreeDTO[0]));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取目录树成功！", root));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取目录树失败！", null));
        }
    }


    @PostMapping("/get-one")
    public ResponseDTO<FolderDTO> getOne(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<FolderIdBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            String id = requestDTO.getData().getId();
            QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", user.getId());

            if (Objects.equals(id, "root")) {
                queryWrapper.isNull("parentId");
            } else {
                queryWrapper.eq("id", id);
            }

            Folder folder = folderService.getOne(queryWrapper);

            if (folder == null || folder.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("文件夹不存在！", null));
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取当前目录信息成功！", FolderDTO.toDTO(folder, noteService, folderService)));
        } catch (Exception e) {
//           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取当前目录信息失败！", null));
        }
    }

    @Data
    @NoArgsConstructor
    public static class ListFolderBody {
        private int page;
        private int size;
        private Query query;

        @Data
        @NoArgsConstructor
        public static class Query {
            private String keyword;
            private String parentId;
            private int isCaseSensitive;
        }
    }

    @Data
    @NoArgsConstructor
    public static class DeleteFolderBody {
        private String id;
    }

    @Data
    @NoArgsConstructor
    public static class CreateFolderBody {
        private String name;
        private String parentId;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateFolderBody {
        private String id;
        private String name;
        private String parentId;
    }

    @Data
    @NoArgsConstructor
    public static class FolderIdBody {
        private String id;
    }
}
