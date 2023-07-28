package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.hibernate.Transaction;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.siteCrawler.SiteCrawler;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SitesList rawSitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private ForkJoinPool pool;

    @Override
    public IndexingResponse startIndexing() {
        siteRepository.deleteAll();

        for (Site site : rawSitesList.getSites()) {
            site.setStatus(Status.INDEXING);
            site.setStatusTime(new Date());
            siteRepository.save(site);
            pool = new ForkJoinPool();
            pool.invoke(new SiteCrawler(site, "/", pageRepository));
        }
        return null;
    }

    @Override
    public boolean stopIndexing() {
        pool.shutdownNow();
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus() == (Status.INDEXING)) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }
        return pool.isShutdown();
    }
}
