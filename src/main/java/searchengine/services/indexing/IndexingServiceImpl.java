package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageInfo;
import searchengine.model.entities.*;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.services.lemmas.LemmaServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaServiceImpl lemmaService;
    private final HtmlParser htmlParser;
    public static boolean stopFlag = false;

    @Override
    public IndexingResponse readAndIndex() {
        if (siteRepository.existsByStatus(Status.INDEXING)) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        List<Site> sitesList = sites.getSites();
        deleteSites();
        for (Site site : sitesList) {
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
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!siteRepository.existsByStatus(Status.INDEXING)) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        stopFlag = true;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexPageFromUrl(String url) {
        String siteUrl = "";
        String path = "/";
        try {
            URL gotUrl = new URL(url);
            siteUrl = gotUrl.getProtocol() + "://" + gotUrl.getHost() + "/";
            path = gotUrl.getPath();
        } catch (MalformedURLException e) {
            log.error("Error at parsing url, ", e);
        }

        path = path.trim();
        path = path.isBlank() ? "/" : path;
        Optional<SiteEntity> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
        if (optional.isPresent()) {
            SiteEntity site = optional.get();
            indexing(site.getId());
            deletePage(site, path);
            parsePage(site, path);
            Optional<PageEntity> optionalPage = pageRepository.findBySiteAndPath(site, path);
            if (optionalPage.isPresent()) {
                PageEntity page = optionalPage.get();

                new Thread(()-> new ThreadHelper(lemmaService, siteRepository, page, site)).start();
                return new IndexingResponse(true);
            }
            log.warn("Page not found: {}", path);
            return new IndexingResponse(false, "Запрашиваемая страница не найдена");
        }
        log.warn("Site not found: {}", siteUrl);
        return new IndexingResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }

    private void parsePage(SiteEntity site, String path) {
        PageInfo pageInfo;
        try {
            pageInfo = htmlParser.getPageInfo(site.getUrl() + path);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        pageRepository.save(PageEntity.builder()
                .site(site)
                .path(path)
                .code(pageInfo.getStatusCode())
                .content(pageInfo.getContent())
                .build());
    }

    private void deletePage(SiteEntity site, String path) {
        Optional<PageEntity> optionalPage = pageRepository.findBySiteAndPath(site, path);

        if (optionalPage.isPresent()) {
            PageEntity page = optionalPage.get();
            List<IndexEntity> indexes = page.getIndexes();
            for (IndexEntity index : indexes) {
                indexRepository.delete(index);
                LemmaEntity lemmaEntity = index.getLemma();
                if (lemmaEntity.getFrequency() <= 0) {
                    lemmaRepository.delete(lemmaEntity);
                } else {
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
                    lemmaRepository.save(lemmaEntity);
                }
            }
            pageRepository.delete(page);
        }
    }

    private void indexing(int siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow(()-> new IllegalStateException("Site not found"));
        site.setStatus(Status.INDEXING);
        siteRepository.save(site);
        log.info("Indexing finished");
    }

    private void deleteSites() {
        log.info("Delete all sites");
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }
}