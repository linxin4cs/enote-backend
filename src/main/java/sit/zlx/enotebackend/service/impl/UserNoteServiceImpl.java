package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.zlx.enotebackend.domain.ActiveNote;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.NoteTag;
import sit.zlx.enotebackend.repository.NoteDocRepository;
import sit.zlx.enotebackend.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class UserNoteServiceImpl implements UserNoteService {
    private final FileService fileService;
    private final ActiveNoteService activeNoteService;
    private final NoteTagService noteTagService;
    private final NoteService noteService;
    private final TagService tagService;
    private final NoteDocRepository noteDocRepository;

    @Autowired
    UserNoteServiceImpl(FileService fileService, ActiveNoteService activeNoteService, NoteTagService noteTagService, NoteService noteService, TagService tagService, NoteDocRepository noteDocRepository) {
        this.fileService = fileService;
        this.activeNoteService = activeNoteService;
        this.noteTagService = noteTagService;
        this.noteService = noteService;
        this.tagService = tagService;
        this.noteDocRepository = noteDocRepository;
    }


    @Override
    @Async
    public CompletableFuture<Void> deleteNoteFiles(List<String> ids) throws IOException {
        for (String id : ids) {
            List<File> files = fileService.list(new QueryWrapper<File>().eq("noteId", id));
            for (File file : files) {
                fileService.removeById(file.getId());
                Files.deleteIfExists(Paths.get(file.getPath()));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async
    public CompletableFuture<Void> deleteNoteRelatedRecords(List<String> ids) {
        // 删除 activeNote
        // 删除 tag 看情况，如果那个 tag 下的 note 数量为 0 了，就删除 tag。删除 tag 需要先删除 noteTag
        // 删除 noteTag
        // 删除 note
        for (String id : ids) {
            activeNoteService.remove(new QueryWrapper<ActiveNote>().eq("noteId", id));
            List<NoteTag> noteTags = noteTagService.list(new QueryWrapper<NoteTag>().eq("noteId", id));
            for (NoteTag noteTag : noteTags) {
                String tagId = noteTag.getTagId();
                noteTagService.remove(new QueryWrapper<NoteTag>().eq("noteId", id));
                if (noteTagService.count(new QueryWrapper<NoteTag>().eq("tagId", tagId)) == 0) {
                    tagService.removeById(noteTag.getTagId());
                }
            }

            noteService.removeById(id);
            noteDocRepository.deleteById(id);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void updateNoteModifiedTime(String noteId, String userId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        Date date = new Date();

        QueryWrapper<ActiveNote> activeNoteQueryWrapper = new QueryWrapper<ActiveNote>().eq("noteId", noteId).eq("modifiedDate", sdf.format(date));
        ActiveNote activeNote = activeNoteService.getOne(activeNoteQueryWrapper);

        if (activeNote == null) {
            activeNote = new ActiveNote();
            activeNote.setNoteId(noteId);
            activeNote.setUserId(userId);
            activeNote.setModifiedDate(date);
            activeNote.setModifiedTime(date);
            activeNoteService.save(activeNote);
        } else {
            activeNote.setModifiedTime(date);
            activeNoteService.update(activeNote, activeNoteQueryWrapper);
        }
    }

    @Async
    @Override
    @Transactional
    public void deleteNoteData(List<String> ids) throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<Void> future = deleteNoteFiles(ids);
        future.get();

        deleteNoteRelatedRecords(ids);

    }
}
