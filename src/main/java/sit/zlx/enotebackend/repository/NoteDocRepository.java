package sit.zlx.enotebackend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sit.zlx.enotebackend.domain.NoteDoc;

public interface NoteDocRepository extends MongoRepository<NoteDoc, String> {

}
