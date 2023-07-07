package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexService {
    public IndexingResponse startIndexing();

    public boolean stopIndexing();
}
