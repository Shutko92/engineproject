package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    public static boolean stopFlag = false;
    HtmlParser htmlParser;
    @Transactional
    public void readLinks() {
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {

            stopFlag = false;

            if (siteRepository.findSiteByUrl(site.getUrl()) != null) {
                siteRepository.deleteSiteByUrl(site.getUrl());
            }
            SiteEntity siteToIndex = indexingBegin(site);
//            log.info("Start indexing");
//            if (existsIndexingSite()) {
//                log.warn("Indexing already start");
//                throw new BadRequestException("Индексация уже запущена");
//            }
//
//            deleteSites();
//            code delete pages method

            Thread thread = new Thread(()-> {
                new ForkJoinPool().invoke(
                        new UrlParser(siteToIndex.getId(), siteToIndex.getUrl(), siteRepository, pageRepository, this, htmlParser, true));
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ConcurrentHashMap<String, Set<String>> linkMap = WebPageScraper.getLinkMap();
        }
    }

    private SiteEntity indexingBegin(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setStatus(Status.INDEXING);
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    public static boolean isStopFlag() {
        return stopFlag;
    }

//    private void deleteSites() {
//        log.info("Delete all sites");
//        indexRepository.deleteAllInBatch();
//        lemmaRepository.deleteAllInBatch();
//        pageRepository.deleteAllInBatch();
//        siteRepository.deleteAllInBatch();
//    }
}