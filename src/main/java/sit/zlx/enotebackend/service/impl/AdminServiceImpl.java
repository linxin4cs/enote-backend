package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.zlx.enotebackend.controller.AdminController;
import sit.zlx.enotebackend.controller.AdminController.BarChartDataBody;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.NoteTag;
import sit.zlx.enotebackend.domain.PersistentLogins;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.repository.NoteDocRepository;
import sit.zlx.enotebackend.repository.SearchHistoryDocRepository;
import sit.zlx.enotebackend.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class AdminServiceImpl implements AdminService {
    private final UserService userService;
    private final PersistentLoginsService persistentLoginsService;
    private final FileService fileService;
    private final ActiveUserService activeUserService;
    private final ActiveNoteService activeNoteService;
    private final NoteTagService notetagService;
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final NoteDocRepository noteDocRepository;
    private final SearchHistoryDocRepository searchHistoryDocRepository;

    @Autowired
    AdminServiceImpl(
            UserService userService,
            PersistentLoginsService persistentLoginsService,
            FileService fileService, ActiveUserService activeUserService,
            ActiveNoteService activeNoteService, NoteService noteService,
            NoteTagService notetagService, FolderService folderService,
            TagService tagService, NoteDocRepository noteDocRepository,
            SearchHistoryDocRepository searchHistoryDocRepository
    ) {
        this.userService = userService;
        this.persistentLoginsService = persistentLoginsService;
        this.fileService = fileService;
        this.activeUserService = activeUserService;
        this.activeNoteService = activeNoteService;
        this.notetagService = notetagService;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.noteDocRepository = noteDocRepository;
        this.searchHistoryDocRepository = searchHistoryDocRepository;
    }

    @Override
    public void formatAndSortBarChartData(BarChartDataBody barChartDataBody, SimpleDateFormat sdf, List<BarChartDataBody.BarChartDataItem> data) {
        for (int i = 0; i < 7; i++) {
            String date = sdf.format(new Date(System.currentTimeMillis() - (6 - i) * 24 * 60 * 60 * 1000));
            if (data.stream().noneMatch(item -> item.getDate().equals(date.substring(5).replace("-", "/")))) {
                AdminController.BarChartDataBody.BarChartDataItem item = new AdminController.BarChartDataBody.BarChartDataItem();
                item.setDate(date.substring(5).replace("-", "/"));
                item.setNum(0L);
                data.add(item);
            }
        }

        data.sort((o1, o2) -> {
            try {
                Date date1 = sdf.parse("2024-" + o1.getDate().replace("/", "-"));
                Date date2 = sdf.parse("2024-" + o2.getDate().replace("/", "-"));
                return date1.compareTo(date2);
            } catch (Exception e) {
                return 0;
            }
        });

        barChartDataBody.setData(data);
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> deleteUserFiles(List<String> ids) throws IOException {
        for (String id : ids) {
            List<File> files = fileService.list(new QueryWrapper<File>().eq("userId", id));
            for (File file : files) {
                fileService.removeById(file.getId());
                Files.deleteIfExists(Paths.get(file.getPath()));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> deleteUserRelatedRecords(List<String> ids) {
        for (String id : ids) {
            User user = userService.getById(id);
            List<String> noteIds = noteService.list(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("userId", id)).stream().map(sit.zlx.enotebackend.domain.Note::getId).toList();

            persistentLoginsService.remove(new QueryWrapper<PersistentLogins>().eq("username", user.getEmail()));
            activeUserService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.ActiveUser>().eq("userId", id));
            if (!noteIds.isEmpty()) {
                activeNoteService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.ActiveNote>().in("noteId", noteIds));
                notetagService.remove(new QueryWrapper<NoteTag>().in("noteId", noteIds));
            }
            noteService.removeByIds(noteIds);
            noteDocRepository.deleteAllById(noteIds);
            folderService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.Folder>().eq("userId", id));
            tagService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.Tag>().eq("userId", id));
            userService.removeById(id);
            searchHistoryDocRepository.deleteById(id);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Override
    @Transactional
    public void deleteUserData(List<String> ids) throws IOException, ExecutionException, InterruptedException {
        deleteUserFiles(ids).get();
        deleteUserRelatedRecords(ids);

    }


}
