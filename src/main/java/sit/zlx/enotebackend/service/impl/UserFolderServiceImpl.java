package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.zlx.enotebackend.domain.Folder;
import sit.zlx.enotebackend.service.FolderService;
import sit.zlx.enotebackend.service.UserFolderService;
import sit.zlx.enotebackend.service.UserNoteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserFolderServiceImpl implements UserFolderService {
    private final FolderService folderService;
    private final UserNoteService userNoteService;

    @Autowired
    public UserFolderServiceImpl(FolderService folderService,   UserNoteService userNoteService) {
        this.folderService = folderService;
        this.userNoteService = userNoteService;
    }

    @Override
    public List<String> getAllSubfolderIds(String folderId) {
        List<String> folderIds = new ArrayList<>();
        getAllSubfolderIdsHelper(folderId, folderIds);
        return folderIds;
    }

    @Override
    public void getAllSubfolderIdsHelper(String folderId, List<String> folderIds) {
        folderIds.add(folderId);
        List<Folder> subfolders = folderService.list(new QueryWrapper<Folder>().eq("parentId", folderId));
        for (Folder subfolder : subfolders) {
            getAllSubfolderIdsHelper(subfolder.getId(), folderIds);
        }
    }

    @Async
    @Override
    @Transactional
    public void deleteFolderData(String folderId, List<String> noteIds) throws IOException, ExecutionException, InterruptedException {
        if (!noteIds.isEmpty()) {
            userNoteService.deleteNoteFiles(noteIds).get();
            userNoteService.deleteNoteRelatedRecords(noteIds).get();
        }

        folderService.removeById(folderId);
    }
}
