package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.repository.SiteRepository;
import searchengine.model.entities.Status;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@Slf4j
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
        return true;
    }
}
//    @Transactional
//    public void deletePage(PageEntity pageEntity) {
//        List<IndexEntity> indexes = pageEntity.getIndexes();
//        for (IndexEntity index : indexes) {
//            LemmaEntity lemma = index.getLemma();
//            if (lemma.getFrequency() <= 1) {
//                lemmaRepository.delete(lemma);
//            } else {
//                lemma.setFrequency(lemma.getFrequency() - 1);
//                lemmaRepository.save(lemma);
//            }
//            indexRepository.delete(index);
//        }
//        pageRepository.delete(pageEntity);
//    }

//запись в главной таблице через дочернюю
//таблица index
//удаление информации о записи из бд
//поле rank
//пропускать ошибочный код
//запись лемм
//statistics