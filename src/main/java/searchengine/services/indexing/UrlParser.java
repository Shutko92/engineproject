package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.UnsupportedMimeTypeException;
import searchengine.dto.indexing.PageInfo;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.Status;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.services.lemmas.LemmaServiceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
@Slf4j
@RequiredArgsConstructor
public class UrlParser extends RecursiveAction {
    private final Integer siteId;
    private final String path;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final HtmlParser htmlParser;
    private final LemmaServiceImpl lemmaService;
    private final boolean isFirstAction;

    @Override
    protected void compute() {
        if (IndexingServiceImpl.isStopFlag()) {
            log.info("Indexing stopped for " + Thread.currentThread().getName());
            failed(siteId, "Индексация остановлена пользователем");
            lemmaService.updateLemmasFrequency(siteId);
            return;
        }
        if (isNotFailed(siteId) && isNotVisited(siteId, path)) {
            try {
                updateStatusTime(siteId);
                Optional<PageEntity> optionalPage = savePage(siteId, path);

                if (optionalPage.isPresent()) {
                    PageEntity page = optionalPage.get();

                    if (page.getCode() < 400) {
                        lemmaService.findAndSave(page);
                    }

                    Set<ForkJoinTask<Void>> tasks = htmlParser.getPaths(page.getContent()).stream()
                            .map(pathFromPage -> new UrlParser(siteId, pathFromPage,
                                    siteRepository, pageRepository,
                                    htmlParser,lemmaService, false).fork())
                            .collect(Collectors.toSet());
                    tasks.forEach(ForkJoinTask::join);

                    if (isFirstAction && isNotFailed(siteId)) {
                        indexed(siteId);
                        lemmaService.updateLemmasFrequency(siteId);
                        log.info("A site finished indexing");
                    }
                }
            } catch (UnsupportedMimeTypeException ignore) {
            } catch (Exception e) {
                IndexingServiceImpl.stopFlag = true;
                log.error("Parser exception", e);
                failed(siteId, "Ошибка парсинга URL: " + getPersistSite(siteId).getUrl() + path);
                lemmaService.updateLemmasFrequency(siteId);
            }
        }
    }

    private boolean isNotFailed(Integer siteId) {
        return !siteRepository.existsByIdAndStatus(siteId, Status.FAILED);
    }

    private boolean isNotVisited(Integer siteId, String path) {
        return !pageRepository.existsBySiteIdAndPath(siteId, path);
    }

    private SiteEntity getPersistSite(Integer siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new IllegalStateException("Site not found"));
    }

    private void updateStatusTime(Integer siteId) {
        SiteEntity persistSite = getPersistSite(siteId);
        persistSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(persistSite);
    }

    private void failed(Integer siteId, String error) {
        IndexingServiceImpl.stopFlag = false;
        log.warn("Failed indexing site with id {}: {}", siteId, error);
        SiteEntity persistSite = getPersistSite(siteId);
        persistSite.setLastError(error);
        persistSite.setStatus(Status.FAILED);
        siteRepository.save(persistSite);
    }

    public Optional<PageEntity> savePage(Integer siteId, String path) throws IOException, InterruptedException {
        SiteEntity site = getPersistSite(siteId);
        PageInfo pageInfo = htmlParser.getPageInfo(site.getUrl() + path);
        if (isNotVisited(siteId, path)) {
            return Optional.of(pageRepository.save(PageEntity.builder()
                    .site(site)
                    .path(path)
                    .code(pageInfo.getStatusCode())
                    .content(pageInfo.getContent())
                    .build()));
        } else {
            return Optional.empty();
        }
    }

    private void indexed(Integer siteId) {
        IndexingServiceImpl.stopFlag = false;
        SiteEntity persistSite = getPersistSite(siteId);
        persistSite.setStatusTime(LocalDateTime.now());
        persistSite.setStatus(Status.INDEXED);
        siteRepository.save(persistSite);
    }
}