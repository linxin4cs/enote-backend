package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.zlx.enotebackend.controller.AdminController;
import sit.zlx.enotebackend.controller.AdminController.BarChartDataBody;
import sit.zlx.enotebackend.controller.AdminController.UsageBody;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.PersistentLogins;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final UserService userService;
    private final PersistentLoginsService persistentLoginsService;
    private final FileService fileService;
    private final ActiveUserService activeUserService;
    private final ActiveNoteService activeNoteService;
    private final NotetagService notetagService;
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;

    @Autowired
    AdminServiceImpl(UserService userService, PersistentLoginsService persistentLoginsService, FileService fileService, ActiveUserService activeUserService, ActiveNoteService activeNoteService, NoteService noteService, NotetagService notetagService, FolderService folderService, TagService tagService) {
        this.userService = userService;
        this.persistentLoginsService = persistentLoginsService;
        this.fileService = fileService;
        this.activeUserService = activeUserService;
        this.activeNoteService = activeNoteService;
        this.notetagService = notetagService;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
    }

    @Override
    public UsageBody.Size getUsageSize(UsageBody.Size totalSize, List<Long> sizes, String parsedTotalSize) {
        UsageBody.Size size = new UsageBody.Size();
        size.setParsedSize(parsedTotalSize);

        if (totalSize.getRawSize() == null) {
            totalSize.setRawSize(0L);  // 如果 rawSize 是 null，则初始化为 0
        }

        // 对 size 的 rawSize 做同样处理
        if (size.getRawSize() == null) {
            size.setRawSize(0L);  // 如果 rawSize 是 null，则初始化为 0
        }

        for (Long sizeLong : sizes) {
            if (sizeLong != null) {  // 这里也检查 sizeLong 是否为 null
                totalSize.setRawSize(totalSize.getRawSize() + sizeLong);
                size.setRawSize(size.getRawSize() + sizeLong);
            }
        }

        return size;
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
    public void deleteUserFiles(List<String> ids) throws IOException {
        for (String id : ids) {
            List<File> files = fileService.list(new QueryWrapper<File>().eq("userId", id));
            for (File file : files) {
                Files.deleteIfExists(Paths.get(file.getPath()));
                fileService.removeById(file.getId());
            }
        }
    }

    @Override
    @Async
    @Transactional
    public void deleteUserRelatedRecords(List<String> ids) {
        for (String id : ids) {
            User user = userService.getById(id);
            List<String> noteIds = noteService.list(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().eq("userId", id)).stream().map(sit.zlx.enotebackend.domain.Note::getId).toList();

            persistentLoginsService.remove(new QueryWrapper<PersistentLogins>().eq("username", user.getEmail()));
            activeUserService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.ActiveUser>().eq("userId", id));
            if (!noteIds.isEmpty()) {
                activeNoteService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.ActiveNote>().in("noteId", noteIds));
                notetagService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.Notetag>().in("noteId", noteIds));
            }
            noteService.removeByIds(noteIds);
            folderService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.Folder>().eq("userId", id));
            tagService.remove(new QueryWrapper<sit.zlx.enotebackend.domain.Tag>().eq("userId", id));
            userService.removeById(id);
        }
    }


}
