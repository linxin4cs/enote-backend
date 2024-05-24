package sit.zlx.enotebackend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import sit.zlx.enotebackend.domain.Tag;
import sit.zlx.enotebackend.domain.*;
import sit.zlx.enotebackend.dto.*;
import sit.zlx.enotebackend.repository.NoteDocRepository;
import sit.zlx.enotebackend.service.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static sit.zlx.enotebackend.service.MyUtils.NoteContentDiffer.compareServerFileIds;


@RestController
@RequestMapping("/api/note")
public class UserNoteController {
    private final UserService userService;
    private final NoteService noteService;
    private final FolderService folderService;
    private final ActiveNoteService activeNoteService;
    private final NoteDocRepository noteDocRepository;
    private final UserNoteService userNoteService;
    private final FileService fileService;
    private final TagService tagService;
    private final NoteTagService noteTagService;
    private final SearchHistoryService searchHistoryService;

    @Autowired
    public UserNoteController(
            UserService userService,
            NoteService noteService,
            FolderService folderService,
            ActiveNoteService activeNoteService,
            NoteDocRepository noteDocRepository,
            UserNoteService userNoteService,
            FileService fileService,
            TagService tagService,
            NoteTagService noteTagService,
            SearchHistoryService searchHistoryService
    ) {
        this.userService = userService;
        this.noteService = noteService;
        this.folderService = folderService;
        this.activeNoteService = activeNoteService;
        this.noteDocRepository = noteDocRepository;
        this.userNoteService = userNoteService;
        this.fileService = fileService;
        this.tagService = tagService;
        this.noteTagService = noteTagService;
        this.searchHistoryService = searchHistoryService;
    }

    private QueryWrapper<Note> getNotesQueryWrapper(String userId, ListNoteBody.Query searchParams) {
        QueryWrapper<Note> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("userId", userId);

        // 先过滤掉正在删除的
        queryWrapper.eq("isDeleting", 0);

        // 根据搜索参数构建查询条件
        if (searchParams != null) {
            if (searchParams.getKeyword() != null && !Objects.equals(searchParams.getKeyword().trim(), "")) {
                if (searchParams.getIsCaseSensitive() == 0) {
                    queryWrapper.like("LOWER(title)", searchParams.getKeyword().trim().toLowerCase());
                } else {
                    queryWrapper.like("title", searchParams.getKeyword().trim());
                }
            }

            if (searchParams.getFolderId() != null) {
                if (searchParams.getFolderId().equals("root")) {
                    queryWrapper.isNull("folderId");
                } else {
                    queryWrapper.eq("folderId", searchParams.getFolderId());
                }
            }

            if (searchParams.getTagId() != null) {
                List<NoteTag> noteTags = noteTagService.list(new QueryWrapper<NoteTag>().eq("tagId", searchParams.getTagId()));

                if (!noteTags.isEmpty()) {
                    queryWrapper.in("id", noteTags.stream().map(NoteTag::getNoteId).toList());
                } else {
                    queryWrapper.in("id", List.of("empty"));
                }


            }

            if (searchParams.getStared() != -1) {
                queryWrapper.eq("stared", searchParams.getStared());
            }


        }

        return queryWrapper;
    }

    @PostMapping("/create")
    public ResponseDTO<NoteItemDTO> create(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<CreateNoteBody> requestDTO) {
        try {
            CreateNoteBody createNoteBody = requestDTO.getData();
            String parsedTitle = createNoteBody.getTitle().trim();
            String folderId = createNoteBody.getFolderId();
            String parsedTitleValidation = MyUtils.Validator.validateNoteTitle(parsedTitle);

            if (!parsedTitleValidation.isEmpty()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(parsedTitleValidation, null));
            }

            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            Note searchNote;

            if (folderId != null) {
                if (folderId.equals("root")) {
                    searchNote = noteService.getOne(new QueryWrapper<Note>().eq("title", parsedTitle).isNull("folderId").eq("userId", user.getId()));

                } else {
                    Folder folder = folderService.getById(folderId);

                    if (folder == null || !Objects.equals(folder.getUserId(), user.getId())) {
                        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("父文件夹不存在！", null));
                    }

                    if (folder.getIsDeleting() == 1) {
                        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("文件夹正在删除！", null));
                    }

                    folderService.updateUpdatedAt(folder.getId());

                    searchNote = noteService.getOne(new QueryWrapper<Note>().eq("title", parsedTitle).eq("folderId", folderId).eq("userId", user.getId()));
                }
            } else {
                searchNote = noteService.getOne(new QueryWrapper<Note>().eq("title", parsedTitle).eq("userId", user.getId()));
            }

            if (searchNote != null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("目录中存在重名笔记！", null));
            }

            Note newNote = new Note();
            String noteId = UUID.randomUUID().toString();
            newNote.setId(noteId);
            newNote.setUserId(user.getId());
            newNote.setTitle(parsedTitle);
            if (!(folderId == null || folderId.equals("root"))) {
                newNote.setFolderId(folderId);
            }
            noteService.save(newNote);

            ActiveNote activeNote = new ActiveNote();
            activeNote.setNoteId(newNote.getId());
            activeNote.setUserId(user.getId());
            activeNoteService.save(activeNote);

            // 检查是否有笔记文档，如果有，进行删除，如果没有，添加一个新的文档
            if (noteDocRepository.existsById(newNote.getId())) {
                noteDocRepository.deleteById(newNote.getId());
            }

            NoteDoc noteDoc = new NoteDoc();
            noteDoc.setId(newNote.getId());
            noteDoc.setHtmlContent("");
            noteDoc.setTextContent("");
            noteDoc.setAttachments(new ArrayList<>());
            noteDocRepository.save(noteDoc);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("创建笔记成功！", NoteItemDTO.toListItemDTO(noteService.getById(noteId), activeNoteService, folderService, noteDocRepository)));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("创建笔记失败！", null));
        }
    }

    @PostMapping("/delete")
    public ResponseDTO<?> delete(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<NoteIdBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            NoteIdBody noteIdBody = requestDTO.getData();
            String noteId = noteIdBody.getId();

            Note note = noteService.getById(noteId);

            if (note == null || !Objects.equals(note.getUserId(), user.getId())) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            if (note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记已在删除队列中！", null));
            }

            if (note.getFolderId() != null) {
                folderService.updateUpdatedAt(note.getFolderId());
            }

            note.setIsDeleting(1);
            noteService.updateById(note);

            userNoteService.deleteNoteData(List.of(noteId));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除笔记成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除笔记失败！", null));
        }
    }

    @PostMapping("/list")
    public ResponseDTO<PaginatedDTO<NoteItemDTO>> list(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<ListNoteBody> requestDTO) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            ListNoteBody pageParams = requestDTO.getData();
            ListNoteBody.Query searchParams = pageParams.getQuery(); // 获取搜索参数
            int page = pageParams.getPage();
            int size = pageParams.getSize();

            Page<Note> pageObj = new Page<>(page, size);

            QueryWrapper<Note> queryWrapper = getNotesQueryWrapper(user.getId(), searchParams);

            // 需要对结果进行排序，但是无法通过Note表中的字段进行排序，需要通过 ActiveNote 表中的 modifiedTime 进行排序，降序排序
            QueryWrapper<ActiveNote> activeNoteQueryWrapper = new QueryWrapper<ActiveNote>().eq("userId", user.getId()).select("noteId").orderByDesc("modifiedTime");
            List<String> ids = activeNoteService.list(activeNoteQueryWrapper).stream()
                    .map(ActiveNote::getNoteId)
                    .toList();
            StringBuilder idsCsv = new StringBuilder();
            for (String id : ids) {
                if (!idsCsv.isEmpty()) idsCsv.append(",");
                idsCsv.append("'").append(id).append("'");
            }

            if (!idsCsv.isEmpty()) {
                queryWrapper.orderBy(true, true, "FIELD(id," + idsCsv + ")");
            }

            IPage<Note> notePage = noteService.page(pageObj, queryWrapper);
            List<Note> notes = notePage.getRecords();

            if (searchParams != null && searchParams.getKeyword() != null && !searchParams.getKeyword().trim().isEmpty()) {
                // 保存搜索记录
                searchHistoryService.saveSearchHistory(user.getId(), searchParams.getKeyword().trim());
            }

            ResponseDTO.ResponseData<PaginatedDTO<NoteItemDTO>> responseData = new ResponseDTO.ResponseData<>("获取笔记列表成功！", new PaginatedDTO<>(notes.stream().map(note -> NoteItemDTO.toListItemDTO(note, activeNoteService, folderService, noteDocRepository)).toList(), (int) notePage.getCurrent(), (int) notePage.getTotal(), (int) notePage.getPages()));

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), responseData);
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记列表失败！", null));
        }
    }

    @PostMapping("/update/rename")
    public ResponseDTO<NoteItemDTO> rename(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<RenameNoteBody> requestDTO) {
        try {
            RenameNoteBody renameNoteBody = requestDTO.getData();
            String parsedTitle = renameNoteBody.getTitle().trim();
            String parsedTitleValidation = MyUtils.Validator.validateNoteTitle(parsedTitle);

            if (!parsedTitleValidation.isEmpty()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(parsedTitleValidation, null));
            }

            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            Note note = noteService.getById(renameNoteBody.getId());

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            note.setTitle(parsedTitle);
            noteService.updateById(note);
            userNoteService.updateNoteModifiedTime(note.getId(), userId);

            Folder folder = folderService.getById(note.getFolderId());

            if (folder != null) {
                folderService.updateUpdatedAt(folder.getId());
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("重命名成功！", NoteItemDTO.toListItemDTO(note, activeNoteService, folderService, noteDocRepository)));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("重命名失败！", null));
        }
    }

    @PostMapping("/update/star")
    public ResponseDTO<NoteItemDTO> star(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<NoteIdBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            Note note = noteService.getById(requestDTO.getData().getId());

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            note.setStared(note.getStared() == 1 ? 0 : 1);
            noteService.updateById(note);
            userNoteService.updateNoteModifiedTime(note.getId(), userId);

            Folder folder = folderService.getById(note.getFolderId());
            if (folder != null) {
                folderService.updateUpdatedAt(folder.getId());
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("操作成功！", NoteItemDTO.toListItemDTO(note, activeNoteService, folderService, noteDocRepository)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("操作失败！", null));
        }
    }

    @PostMapping("/update/move")
    public ResponseDTO<MoveDTO> move(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<MoveNoteBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            Note note = noteService.getById(requestDTO.getData().getId());

            if (note == null || !Objects.equals(note.getUserId(), userId)) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            if ((requestDTO.getData().getFolderId() == null || Objects.equals(requestDTO.getData().getFolderId(), "root"))) {
                note.setFolderId(null);
            } else {
                note.setFolderId(requestDTO.getData().getFolderId());

                Folder folder = folderService.getById(requestDTO.getData().getFolderId());

                if (folder == null || !Objects.equals(folder.getUserId(), userId)) {
                    return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("文件夹不存在！", null));
                }

                if (note.getIsDeleting() == 1) {
                    return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记正在删除！", null));
                }

                if (folder.getIsDeleting() == 1) {
                    return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("目标文件夹正在删除！", null));
                }
            }

            noteService.updateById(note);
            userNoteService.updateNoteModifiedTime(note.getId(), userId);

            MoveDTO moveDTO = new MoveDTO();

            if (note.getFolderId() == null) {
                moveDTO.setFolderId("root");
            } else {

                Folder oldFolder = folderService.getById(note.getFolderId());
                if (oldFolder != null) {
                    folderService.updateUpdatedAt(oldFolder.getId());
                }

                Folder newFolder = folderService.getById(note.getFolderId());
                if (newFolder != null) {
                    folderService.updateUpdatedAt(newFolder.getId());
                }

                moveDTO.setFolderId(note.getFolderId());
            }
            if (note.getFolderId() == null) {
                moveDTO.setFolderName("我的文件夹");
            } else {
                moveDTO.setFolderName(folderService.getById(note.getFolderId()).getName());
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("移动成功！", moveDTO));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("移动失败！", null));
        }
    }

    @PostMapping("/update/content")
    public ResponseDTO<?> updateContent(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<UpdateNoteContentBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            UpdateNoteContentBody updateNoteContentBody = requestDTO.getData();
            Note note = noteService.getById(updateNoteContentBody.getId());

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            NoteDoc noteDoc = noteDocRepository.findById(note.getId()).orElse(null);

            if (noteDoc == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            Set<String> missingFileIds = compareServerFileIds(noteDoc.getHtmlContent(), updateNoteContentBody.getHtmlContent());

            if (!missingFileIds.isEmpty()) {
                for (String missingFileId : missingFileIds) {
                    Files.deleteIfExists(Paths.get(fileService.getById(missingFileId).getPath()));
                }
                fileService.removeByIds(missingFileIds.stream().toList());
                noteDoc.setAttachments(noteDoc.getAttachments().stream().filter(id -> !missingFileIds.contains(id)).toList());
            }

            noteDoc.setHtmlContent(updateNoteContentBody.getHtmlContent());
            noteDoc.setTextContent(updateNoteContentBody.getTextContent());
            noteDocRepository.save(noteDoc);

            userNoteService.updateNoteModifiedTime(note.getId(), userId);

            Folder folder = folderService.getById(note.getFolderId());
            if (folder != null) {
                folderService.updateUpdatedAt(folder.getId());
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("保存成功！", null));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("保存失败！", null));
        }
    }

    @GetMapping("/get-tree")
    public ResponseDTO<FolderTreeDTO> getTree(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            List<Folder> folders = folderService.list(new QueryWrapper<Folder>().eq("userId", user.getId()).eq("isDeleting", 0));

            // 按照文件夹名称排序
            folders.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

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

    @PostMapping("/get-content")
    public ResponseDTO<NoteContentDTO> getContent(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<NoteIdBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();
            Note note = noteService.getById(requestDTO.getData().getId());


            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            NoteDoc noteDoc = noteDocRepository.findById(note.getId()).orElse(null);

            if (noteDoc == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记内容失败！", null));
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记内容成功！", NoteContentDTO.toDTO(note, noteDoc, activeNoteService, folderService)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记内容失败！", null));
        }
    }

    @PostMapping("/get-more-info")
    public ResponseDTO<NoteItemDTO> getMoreInfo(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<NoteIdBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            System.out.println(requestDTO.getData());
            Note note = noteService.getById(requestDTO.getData().getId());

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记更多信息成功！", NoteItemDTO.toMoreInfoDTO(note, activeNoteService, fileService, noteDocRepository, tagService, noteTagService, folderService)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记更多信息失败！", null));
        }
    }

    @PostMapping("/delete-tag")
    public ResponseDTO<NoteItemDTO> deleteTag(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<DeleteTagBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            DeleteTagBody deleteTagBody = requestDTO.getData();
            String noteId = deleteTagBody.getNoteId();
            String tagId = deleteTagBody.getTagId();

            Note note = noteService.getById(noteId);

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            NoteTag noteTag = noteTagService.getOne(new QueryWrapper<NoteTag>().eq("noteId", noteId).eq("tagId", tagId));

            if (noteTag == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("标签不存在！", null));
            }


            noteTagService.remove(new QueryWrapper<NoteTag>().eq("noteId", noteId).eq("tagId", tagId));

            if (noteTagService.count(new QueryWrapper<NoteTag>().eq("tagId", tagId)) == 0) {
                tagService.removeById(noteTag.getTagId());
            }

            userNoteService.updateNoteModifiedTime(noteId, userId);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除标签成功！", NoteItemDTO.toListItemDTO(note, activeNoteService, folderService, noteDocRepository)));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除标签失败！", null));
        }
    }

    @PostMapping("/add-tag")
    public ResponseDTO<String> addTag(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<AddTagBody> requestDTO) {
        try {
            String userId = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername())).getId();

            AddTagBody addTagBody = requestDTO.getData();
            String noteId = addTagBody.getNoteId();
            String tagName = addTagBody.getTagName();

            Note note = noteService.getById(noteId);

            if (note == null || !Objects.equals(note.getUserId(), userId) || note.getIsDeleting() == 1) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("笔记不存在！", null));
            }

            Tag tag = tagService.getOne(new QueryWrapper<Tag>().eq("name", tagName).eq("userId", userId));

            if (tag == null) {
                String tagNameValidation = MyUtils.Validator.validateTagName(tagName);
                if (!tagNameValidation.isEmpty()) {
                    return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(tagNameValidation, null));
                }


                tag = new Tag();
                tag.setId(UUID.randomUUID().toString());
                tag.setName(tagName.trim());
                tag.setUserId(userId);
                tagService.save(tag);
            }

            NoteTag noteTag = noteTagService.getOne(new QueryWrapper<NoteTag>().eq("noteId", noteId).eq("tagId", tag.getId()));

            if (noteTag != null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("标签已存在！", null));
            }

            noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tag.getId());
            noteTagService.save(noteTag);

            userNoteService.updateNoteModifiedTime(noteId, userId);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("添加标签成功！", tag.getId()));
        } catch (Exception e) {
           
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("添加标签失败！", null));
        }
    }


    @Data
    @NoArgsConstructor
    public static class ListNoteBody {
        private int page;
        private int size;
        private Query query;

        @Data
        @NoArgsConstructor
        public static class Query {
            private String keyword;
            private String folderId;
            private String tagId;
            private int stared;
            private int isCaseSensitive;
        }
    }

    @Data
    @NoArgsConstructor
    public static class CreateNoteBody {
        private String title;
        private String folderId;
    }

    @Data
    @NoArgsConstructor
    public static class NoteIdBody {
        private String id;
    }

    @Data
    @NoArgsConstructor
    public static class RenameNoteBody {
        private String id;
        private String title;
    }

    @Data
    @NoArgsConstructor
    public static class MoveNoteBody {
        private String id;
        private String folderId;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateNoteContentBody {
        private String id;
        private String htmlContent;
        private String textContent;
    }

    @Data
    @NoArgsConstructor
    public static class MoveDTO {
        private String folderId;
        private String folderName;
    }

    @Data
    @NoArgsConstructor
    public static class DeleteTagBody {
        private String noteId;
        private String tagId;
    }

    @Data
    @NoArgsConstructor
    public static class AddTagBody {
        private String noteId;
        private String tagName;
    }
}
