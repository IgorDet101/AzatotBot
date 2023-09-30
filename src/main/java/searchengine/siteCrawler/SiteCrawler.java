package searchengine.siteCrawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class SiteCrawler extends RecursiveAction {
    private static final Set<String> passedAddressSet = Collections.synchronizedSet(new HashSet<>());
    private final Site site;
    private final String rootUrl;
    private final String fullUrl;
    private final String shortUrl;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public SiteCrawler(Site site, String shortUrl, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.site = site;
        rootUrl = site.getUrl();
        fullUrl = rootUrl + shortUrl;
        this.shortUrl = shortUrl;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;

        if (shortUrl.equals("/")) {
            passedAddressSet.add(shortUrl);
        }
    }

    private Connection getConnection() throws InterruptedException {
        sleep(500);
        Connection connection = Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                        "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");

        return connection;
    }

    private Document getDocument(Connection connection) throws Exception {
        Document doc;
        if (connection != null) doc = connection.get();
        else throw new NullPointerException("Exception: connection is null");
        return doc;
    }

    @Override
    protected void compute() {
        Document document = null;
        try {
            Connection connection = getConnection();
            document = getDocument(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SinglePageIndexing singlePageIndexing = new SinglePageIndexing(site, shortUrl, document, pageRepository, lemmaRepository, indexRepository);
        singlePageIndexing.run();

        createSubTask(document);
    }

    private synchronized void createSubTask(Document doc) {
        Set<String> urlSet = doc.select("a").stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> e.startsWith(rootUrl))
                .filter(e -> e.endsWith("/") | (e.endsWith("html")))
                .map(e -> e.substring(rootUrl.length()))
                .filter(e -> !passedAddressSet.contains(e))
                .collect(Collectors.toSet());


        if (urlSet.size() >= 1) {
            passedAddressSet.addAll(urlSet);
            List<SiteCrawler> taskList = new ArrayList<>();
            for (String shortUrl : urlSet) {
                SiteCrawler siteCrawler = new SiteCrawler(site, shortUrl, pageRepository, lemmaRepository, indexRepository);
                taskList.add(siteCrawler);
            }
            invokeAll(taskList);
            for (SiteCrawler task : taskList) {
                task.join();
            }
        }
    }

    public static void clearAddressSet() {
        passedAddressSet.clear();
    }
}
