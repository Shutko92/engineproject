package searchengine.services.lemmas;

import searchengine.model.entities.PageEntity;

public interface LemmaService {
    void findAndSave(PageEntity page);
    void updateLemmasFrequency(Integer siteId);
}
