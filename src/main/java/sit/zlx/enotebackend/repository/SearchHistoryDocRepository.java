package sit.zlx.enotebackend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sit.zlx.enotebackend.domain.SearchHistoryDoc;

public interface SearchHistoryDocRepository extends MongoRepository<SearchHistoryDoc, String> {
}
