package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.searching.SearchServiceImpl;
import searchengine.services.statistics.StatisticsServiceImpl;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final IndexingServiceImpl indexingService;
    private final StatisticsServiceImpl statisticsService;
    private final SearchServiceImpl searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.readAndIndex());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public  ResponseEntity<IndexingResponse> indexPage(String url) {
        return ResponseEntity.ok(indexingService.indexPageFromUrl(url));
    }

    @GetMapping("/search")
    public  ResponseEntity<SearchResponse> search(@RequestParam(name = "query", defaultValue = "") String query, @RequestParam(name = "site", defaultValue = "") String site,
                                 @RequestParam(name = "offset", defaultValue = "0") int offset, @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (query.isEmpty()) {
            return ResponseEntity.ok(new SearchResponse(false, "Задан пустой поисковый запрос"));
        }
        return ResponseEntity.ok(searchService.processSearch(query, site, offset, limit));
    }
}