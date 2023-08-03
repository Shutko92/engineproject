package searchengine.dto.searching;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SearchResponse {
    private boolean result;
    private String error;
    private int count;
    private List<SearchInfo> data;

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public SearchResponse(boolean result, int count, List<SearchInfo> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
