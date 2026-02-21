package searchengine.services;

import searchengine.dto.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
}
