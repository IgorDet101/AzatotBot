package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.RawSite;
import searchengine.model.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.siteCrawler.SinglePageIndexing;
import searchengine.siteCrawler.SiteCrawler;

import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SitesList rawSitesList;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private boolean isIndexing = false;
    private final ForkJoinPool pool = new ForkJoinPool();

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (isIndexing) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }

        siteRepository.deleteAll();
        isIndexing = true;
        for (RawSite rawSite : rawSitesList.getRawSites()) {
            Site site = createNewSite(rawSite.getName(), rawSite.getUrl());
            siteRepository.save(site);
            pool.invoke(new SiteCrawler(site, "/", pageRepository));
            site.setStatus(Status.INDEXED);
        }
        response.setResult(true);
        isIndexing = false;
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (!isIndexing) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }
        pool.shutdownNow();
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() == (Status.INDEXING)) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }
        SiteCrawler.clearAddressSet();
        isIndexing = false;
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        IndexingResponse indexingResponse = new IndexingResponse();
        boolean match = false;

        for (RawSite rawSite : rawSitesList.getRawSites()) {
            String rootUrl = rawSite.getUrl();
            if (url.startsWith(rootUrl)) {
                match = true;
                Site site = siteRepository.findByUrl(rootUrl);
                if (site == null) {
                    site = createNewSite(rawSite.getName(), rootUrl);
                    siteRepository.save(site);
                }
                String shortUrl = url.substring(rootUrl.length());
                SinglePageIndexing singlePage = new SinglePageIndexing(site, shortUrl, pageRepository, lemmaRepository, indexRepository);
                pool.execute(singlePage);
                site.setStatus(Status.INDEXED);
                siteRepository.save(site);
            }
        }

        if (match) {
            indexingResponse.setResult(match);
            return indexingResponse;
        } else {
            indexingResponse.setResult(match);
            indexingResponse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return indexingResponse;
        }
    }

    private Site createNewSite(String name, String url) {
        Site site = new Site();
        site.setName(name);
        site.setUrl(url);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        return site;
    }
}
