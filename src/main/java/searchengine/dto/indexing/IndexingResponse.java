package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String Error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
}
