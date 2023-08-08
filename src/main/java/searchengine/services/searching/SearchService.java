package searchengine.services.searching;

import searchengine.dto.searching.SearchResponse;

public interface SearchService {
    SearchResponse processSearch(String query, String site, int offset, int limit);
}
