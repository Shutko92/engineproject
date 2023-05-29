package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteRepository;
import searchengine.model.Status;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private final StatisticsService statisticsService;
    @Autowired
    private SiteRepository siteRepository;
    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

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
}