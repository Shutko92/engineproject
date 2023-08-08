package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse readAndIndex();
    IndexingResponse indexPageFromUrl(String url);
    IndexingResponse stopIndexing();
}
