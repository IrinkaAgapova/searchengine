package searchengine.dto;
import searchengine.model.Page;

public record PageRelevance(
        Page page,
        Double absoluteRelevance,
        Double relativeRelevance
) {

}
