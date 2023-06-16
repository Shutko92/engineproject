package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    public static boolean stopFlag = false;
    @Autowired
    private final HtmlParser htmlParser;
@Transactional
    public void readLinks() {
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {

            if (siteRepository.findByUrl(site.getUrl()) != null) {
                siteRepository.removeByUrl(site.getUrl());
            }
            String url = site.getUrl();

            log.info("Save site with url: {}", url);
            siteRepository.save(SiteEntity.builder()
                    .name(site.getName())
                    .status(Status.INDEXING)
                    .url(url.toLowerCase())
                    .statusTime(LocalDateTime.now())
                    .build());
        }

        for (SiteEntity site : siteRepository.findAll()) {
            new Thread(()-> new ForkJoinPool().invoke(
                    new UrlParser(site.getId(), site.getUrl(), siteRepository, pageRepository, this, htmlParser, true))).start();
        }
    }

    public static boolean isStopFlag() {
        return stopFlag;
    }
//            SiteEntity siteToIndex = indexingBegin(site);
//            log.info("Start indexing");
//            if (existsIndexingSite()) {
//                log.warn("Indexing already start");
//                throw new BadRequestException("Индексация уже запущена");
//            }
//
//            deleteSites();
//    private void deleteSites() {
//        log.info("Delete all sites");
//        indexRepository.deleteAllInBatch();
//        lemmaRepository.deleteAllInBatch();
//        pageRepository.deleteAllInBatch();
//        siteRepository.deleteAllInBatch();
//    }
}