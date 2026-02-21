package searchengine.services;

import searchengine.dto.ResponseSearch;

public interface SearchService {
    ResponseSearch systemSearch(String query, String site, Integer offset, Integer limit);

}
