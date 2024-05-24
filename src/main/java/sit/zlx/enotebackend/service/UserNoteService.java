package sit.zlx.enotebackend.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public interface UserNoteService {
    CompletableFuture<Void> deleteNoteFiles(List<String> ids) throws IOException;
    CompletableFuture<Void> deleteNoteRelatedRecords(List<String> ids);
    void updateNoteModifiedTime(String noteId, String userId);
    void deleteNoteData(List<String> ids) throws IOException, ExecutionException, InterruptedException;
}
