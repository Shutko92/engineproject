package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.Status;
import searchengine.model.repository.SiteRepository;
import searchengine.services.lemmas.LemmaServiceImpl;

@Slf4j
@AllArgsConstructor
public class ThreadHelper implements Runnable{
    private final LemmaServiceImpl lemmaService;
    private final SiteRepository siteRepository;
    private final PageEntity page;
    private final SiteEntity site;

    @Override
    public void run() {
        lemmaService.findAndSave(page);
        lemmaService.updateLemmasFrequency(site.getId());
        indexed(site.getId());
    }

    private void indexed(int siteId) {
        SiteEntity site = siteRepository.findById(siteId).orElseThrow(()-> new IllegalStateException("Site not found"));
        site.setStatus(Status.INDEXED);
        siteRepository.save(site);
        log.info("Indexing finished");
    }
}
