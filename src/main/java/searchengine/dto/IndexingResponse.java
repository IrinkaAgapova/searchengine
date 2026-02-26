package searchengine.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class IndexingResponse {
    private boolean result;

    public IndexingResponse(boolean result) {
        this.result = result;
    }

}
