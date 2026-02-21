package searchengine.dto;

public record ResultSearch(String url,
                           String nameUrl,
                           String uri,
                           String title,
                           String snippet,
                           Double relevance) {
    }

