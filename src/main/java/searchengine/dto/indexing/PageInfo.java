package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageInfo {
    private String content;
    private Integer statusCode;
}
