package sit.zlx.enotebackend.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "note")
public class NoteDoc {
    @Id
    @Indexed(unique = true)
    private String id;
    private String htmlContent;
    private String textContent;
    private List<String> attachments;
}
