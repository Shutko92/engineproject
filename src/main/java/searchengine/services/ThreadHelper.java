package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.entities.PageEntity;

@Slf4j
@RequiredArgsConstructor
public class ThreadHelper implements Runnable{
    private final LemmaService lemmaService;
    private final PageEntity page;
    private final int siteId;

    @Override
    public void run() {
        lemmaService.findAndSave(page);
        lemmaService.updateLemmasFrequency(siteId);
    }
}
