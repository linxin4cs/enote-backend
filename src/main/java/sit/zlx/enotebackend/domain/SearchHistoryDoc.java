package sit.zlx.enotebackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "searchHistory")
public class SearchHistoryDoc {
    @Id
    @Indexed(unique = true)
    private String id;
    private List<Keyword> keywords;

    @Data
    @Document
    @AllArgsConstructor
    static
    public class Keyword {
        @Indexed(unique = true)
        private String content;
        private Instant updatedAtInstant;
    }
}


