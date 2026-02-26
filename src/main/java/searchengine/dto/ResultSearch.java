package searchengine.dto;

public record ResultSearch(String site,
                           String siteName,
                           String uri,
                           String title,
                           String snippet,
                           Double relevance) {
    }


