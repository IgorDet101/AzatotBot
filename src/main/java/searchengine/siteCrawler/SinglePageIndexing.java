package searchengine.siteCrawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class SinglePageIndexing implements Runnable {

    private final Site site;
    private final String fullUrl;
    private final String shortUrl;
    private int connectionStatusCode;
    private final Lemmatizer lemmatizer = new Lemmatizer();
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public SinglePageIndexing(Site site, String shortUrl, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.site = site;
        String rootUrl = site.getUrl();
        fullUrl = rootUrl + shortUrl;
        this.shortUrl = shortUrl;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    private Connection getConnection() throws InterruptedException, IOException {
        sleep(500);
        Connection connection = Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                        "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
        connectionStatusCode = connection.execute().statusCode();

        return connection;
    }

    private Document getDocument(Connection connection) throws Exception {
        Document doc;
        if (connection != null) doc = connection.get();
        else throw new NullPointerException("Exception: connection is null");
        return doc;
    }

    @Override
    public void run() {
        Document document = null;
        try {
            Connection connection = getConnection();
            document = getDocument(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Page page = createPage();
        page.setPath(shortUrl);
        page.setSite(site);
        page.setCode(connectionStatusCode);
        page.setContent(document.html());

        pageRepository.save(page);
        getLemmasAndIndex(page);
    }

    private Page createPage() {
        Page page = pageRepository.findByPath(shortUrl);
        if (page != null) {
            pageRepository.delete(page);
        }
        return new Page();
    }

    private void getLemmasAndIndex(Page page) {
        Map<String, Integer> lemmas = lemmatizer.getLemmas(page.getContent());
        List<Index> indexList = new ArrayList<>();

        for (String word : lemmas.keySet()) {
            Lemma lemma;
            synchronized (lemmaRepository) {
                lemma = lemmaRepository.findByLemmaAndSite(word, site);
                if (lemma == null) {
                    lemma = new Lemma();
                    lemma.setLemma(word);
                    lemma.setFrequency(1);
                    lemma.setSite(site);
                } else {
                    lemma.incrementFrequency();
                }
                lemmaRepository.save(lemma);
            }

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(lemmas.get(word));
            indexList.add(index);
        }
        indexRepository.saveAll(indexList);
    }
}
