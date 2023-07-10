package searchengine.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.repository.SiteRepository;
import searchengine.model.entities.Status;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private IndexingService indexingService;
    private final StatisticsService statisticsService;
    private SiteRepository siteRepository;
    private SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {

        if (siteRepository.existsByStatus(Status.INDEXING)) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        indexingService.readLinks();
        return new IndexingResponse(true);
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {

        if (!siteRepository.existsByStatus(Status.INDEXING)) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        IndexingService.stopFlag = true;
        return new IndexingResponse(true);
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(String url) {

        boolean correct = indexingService.getPageFromUrl(url);
        if (!correct) {
            return new IndexingResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return new IndexingResponse(true);
    }

    @GetMapping("/search")
    public boolean search(String query, String site, int offset, int limit) {
        searchService.startSearch(query, site, offset, limit);
        return true;
    }
}