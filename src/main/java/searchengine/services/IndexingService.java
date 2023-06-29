package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaService lemmaService;
    private final HtmlParser htmlParser;
    public static boolean stopFlag = false;

    public void readLinks() {
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {

            deleteSites();
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
                    new UrlParser(site.getId(), site.getUrl(), siteRepository,
                            pageRepository, htmlParser, lemmaService, true))).start();
        }
    }

    public boolean getPageFromUrl(String siteUrl, String path) {
        log.warn("{} should be send to index", siteUrl);
        Optional<SiteEntity> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
        if (optional.isPresent()) {
            SiteEntity site = optional.get();
            PageEntity page = pageRepository.findBySiteAndPath(site, path);

            indexing(site.getId());
            deletePage(site, path);
            runParser(site.getId(), path);
            lemmaService.findAndSave(page);
            lemmaService.updateLemmasFrequency(site.getId());
        } else {
            log.warn("Site not found: {}", siteUrl);
            return false;
        }

        log.warn("Indexing for {}", siteUrl);
        return true;
    }

    private void runParser(int siteId, String path) {
        new UrlParser(siteId, path, siteRepository, pageRepository, htmlParser,lemmaService, true).fork();
    }

    @Transactional
    private void deletePage(SiteEntity site, String path) {
        PageEntity pageEntity = pageRepository.findBySiteAndPath(site, path);
//        optional.ifPresent(pageRepository::delete);
        List<IndexEntity> indexes = pageEntity.getIndexes();
        for (IndexEntity index : indexes) {
            LemmaEntity lemmaEntity = index.getLemma();
            if (lemmaEntity.getFrequency() <= 1) {
                lemmaRepository.delete(lemmaEntity);
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
                lemmaRepository.save(lemmaEntity);
            }
            indexRepository.delete(index);
        }
        pageRepository.delete(pageEntity);
    }

    private void indexing(int siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow(()-> new IllegalStateException("Site not found"));
        site.setStatus(Status.INDEXING);
        siteRepository.save(site);
    }

    public static boolean isStopFlag() {
        return stopFlag;
    }

    private void deleteSites() {
        log.info("Delete all sites");
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }
}