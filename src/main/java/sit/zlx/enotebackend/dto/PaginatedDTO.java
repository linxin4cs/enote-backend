package sit.zlx.enotebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedDTO<T> {
    private List<T> list;
    private int currentPage;
    private int totalItems;
    private int totalPages;
}
