package searchengine.repositories;

import org.springframework.stereotype.Repository;
import searchengine.model.Page;
@Repository
public interface CustPageRepository<Page> {
    void delete(Page page);
}
