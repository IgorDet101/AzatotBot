package searchengine.siteCrawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class SiteCrawler extends RecursiveAction {
    private static final Set<String> passedAddressSet = Collections.synchronizedSet(new HashSet<>());
//    private static final Set<String> addedLemmas = Collections.synchronizedSet(new HashSet<>());
    private final Site site;
    private final String rootUrl;
    private final String fullUrl;
    private final String shortUrl;
    private int connectionStatusCode;
//    private final Lemmatizer lemmatizer = new Lemmatizer();
    private final PageRepository pageRepository;
//    private CriteriaBuilder builder;

    public SiteCrawler(Site site, String shortUrl, PageRepository pageRepository) {
        this.site = site;
        rootUrl = site.getUrl();
        fullUrl = rootUrl + shortUrl;
        this.shortUrl = shortUrl;

        if (shortUrl == "/") {
            passedAddressSet.add(shortUrl);
        }
        this.pageRepository = pageRepository;
    }

    private Connection getConnection() {
        Connection connection = null;
        try {
            sleep(500);
            connection = Jsoup.connect(fullUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                            "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com");
            connectionStatusCode = connection.execute().statusCode();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return connection;
    }

    @Override
    protected void compute() {
        Connection connection = getConnection();

        Document doc = null;
        try {
            doc = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (doc == null) {
            return;
        }

        site.setStatusTime(new Date());
        Page page = new Page();
        page.setPath(shortUrl);
        page.setSite(site);
        page.setCode(connectionStatusCode);
        page.setContent(doc.html());
        pageRepository.save(page);

        createSubTask(doc);
//        if (connectionStatusCode == 200) {
//            List<Field> fieldList = session.createQuery("From Field", Field.class).getResultList();
//            for (Field field : fieldList) {
//                String html = Objects.requireNonNull(doc.selectFirst(field.getSelector())).html();
//                Map<String, Integer> lemmas = lemmatizer.getLemmas(html);
//
//                for (String word : lemmas.keySet()) {
//                    Lemma lemma = addLemmaToDB(word);
//
//                    //create index or change his rank
//                    float rank = field.getWeight() * lemmas.get(word);
//                    addIndexToDB(page, lemma, rank);
//                }
//            }
//        }
    }

//    private void addIndexToDB(Page page, Lemma lemma, float rank) {
//        CriteriaQuery<Index> query = builder.createQuery(Index.class);
//        Root<Index> root = query.from(Index.class);
//        query.where(builder.and(builder.equal(root.get("page"), page),
//                builder.equal(root.get("lemma"), lemma)));
//        Index index = session.createQuery(query).getSingleResultOrNull();
//
//        session.beginTransaction();
//        if (index != null) {
//            rank += index.getRank();
//            index.setRank(rank);
//            session.merge(index);
//        } else {
//            index = new Index(page, lemma, rank);
//            session.persist(index);
//        }
//        session.getTransaction().commit();
//    }

//    private Lemma addLemmaToDB(String word) {
//        Lemma lemma = null;
//
//        if (addedLemmas.contains(word)) {
//            lemma = getLemmaFromDB(word);
//        } else {
//            addedLemmas.add(word);
//        }
//
//        session.beginTransaction();
//        if (lemma == null) {
//            lemma = new Lemma(word);
//            session.persist(lemma);
//        } else {
//            lemma.increaseFrequency();
//            session.merge(lemma);
//        }
//        session.getTransaction().commit();
//        return lemma;
//    }

//    private Lemma getLemmaFromDB(String word) {
//        CriteriaQuery<Lemma> query = builder.createQuery(Lemma.class);
//        Root<Lemma> root = query.from(Lemma.class);
//        query.where(builder.equal(root.get("lemma"), word));
//        try {
//            return session.createQuery(query).getSingleResult();
//        } catch (NoResultException ex) {
//            try {
//                sleep(100);
//                return session.createQuery(query).getSingleResult();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

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
                SiteCrawler siteCrawler = new SiteCrawler(site, shortUrl, pageRepository);
                taskList.add(siteCrawler);
            }
            invokeAll(taskList);
            for (SiteCrawler task : taskList) {
                task.join();
            }
        }
    }


}
