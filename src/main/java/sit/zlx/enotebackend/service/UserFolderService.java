package sit.zlx.enotebackend.service;


import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface UserFolderService {
    List<String> getAllSubfolderIds(String folderId);
    void getAllSubfolderIdsHelper(String folderId, List<String> folderIds);
    void deleteFolderData(String folderId, List<String> noteIds) throws IOException, ExecutionException, InterruptedException;
}
