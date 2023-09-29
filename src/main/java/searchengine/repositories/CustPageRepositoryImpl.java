package searchengine.repositories;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

public class CustPageRepositoryImpl implements CustPageRepository<Page>{
    @Override
    public void delete(Page page) {
        page.getIndexes().stream()
                .map(Index::getLemma)
                .forEach(Lemma::decrementFrequency);
    }
}
