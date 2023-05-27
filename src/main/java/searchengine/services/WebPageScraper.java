package searchengine.services;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.JsoupConfig;
import searchengine.model.PageEntity;
import searchengine.model.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


public class WebPageScraper extends RecursiveTask<Map<String, Set<String>>> {
    public static String URL;
    private static CopyOnWriteArraySet<String> linkSet = new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<String, Set<String>> linkMap = new ConcurrentHashMap<>();
    private String currentPage = "";
    @Autowired
    private PageRepository pageRepository;
    IndexingService indexingService;
    private JsoupConfig jsoupConfig;

    public WebPageScraper(String currentPage) {
        this.currentPage = currentPage;
    }
    public WebPageScraper(String url, String currentPage) {
        URL = url;
        this.currentPage = currentPage;
    }

    public static ConcurrentHashMap<String, Set<String>> getLinkMap() {
        return linkMap;
    }

    @Override
    protected Map<String, Set<String>> compute() {
//        if (indexingService.isStopFlag()) {
//            return;
//        }
        Set<String> subLinks = new HashSet<>();

        List<WebPageScraper> webPageScrapers = new ArrayList<>();
        if (!linkSet.contains(currentPage)) {
            Set<String> links = crawl(currentPage);
            for (String link: links) {
                if (link.contains(currentPage) && !link.equals(currentPage)) {
                    subLinks.add(link);

                    PageEntity pageEntity = new PageEntity();
                    Document html = Jsoup.parse(URL);
                    pageEntity.setContent(html.text());
                    pageEntity.setCode(200);
                    pageEntity.setPath(link);

//                    Connection.Response response = Jsoup.connect(url)
//                            .maxBodySize(0)
//                            .userAgent(jsoupConfig.getUserAgent())
//                            .referrer(jsoupConfig.getReferrer())
//                            .header("Accept-Language", "ru")
//                            .ignoreHttpErrors(true)
//                            .execute();
//                    int statusCode = response.statusCode();

                    pageRepository.save(pageEntity);
                }
            }
            if (!subLinks.isEmpty()) {
                linkMap.put(currentPage, subLinks);
            }
            linkSet.add(currentPage);

            boolean containsAll = linkSet.containsAll(links);
            if (!containsAll) {
                for (String link2: links) {
                    WebPageScraper task =new WebPageScraper(link2);
                    task.fork();
                    webPageScrapers.add(task);
                }
                for(WebPageScraper task: webPageScrapers){
                    task.join();
                }
                ForkJoinTask.invokeAll(webPageScrapers);
            }
        }
        return linkMap;
    }

    private Set<String> crawl(String url) {
        Set<String> set = new HashSet<>();
        try {
            Thread.sleep(200);
            Document doc = request(url);

            if (doc != null) {
                Elements element = doc.select("a[href]");
                for (Element link : element) {
                    String nextLink = link.absUrl("href");
                    if (nextLink.contains(URL) && !nextLink.contains("#") && !nextLink.contains("+")) {
                        set.add(nextLink);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return set;
    }

    public Document request(String url) {
        try {
            Connection con = Jsoup.connect(url)
                    .userAgent(jsoupConfig.getUserAgent()).referrer(jsoupConfig.getReferrer())
                    .header("Accept-Language", "ru").ignoreHttpErrors(true);

            Document doc = con.get();

            int statusCode = con.execute().statusCode();

            if (con.response().statusCode() == 200) {
                return doc;
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}