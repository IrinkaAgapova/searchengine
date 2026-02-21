package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ResponseSearch {

    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer count;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ResultSearch> data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;


    public ResponseSearch(Boolean result, String error) {
        this.result = result;
        this.error = error;
    }
    public ResponseSearch(Boolean result, int count, List<ResultSearch> data) {
        this.result = result;
        this.count = count;
        this.data = data;

    }

}


