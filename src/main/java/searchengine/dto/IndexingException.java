package searchengine.dto;

public class IndexingException extends RuntimeException {
    public IndexingException(String message) {
        super(message);
    }
}
