package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

    @Data
    public class Response{
        private boolean result;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String error;

        public Response(boolean result, String error) {
            this.result = result;
            this.error = error;
        }

        public Response(boolean result) {
                this.result = result;
            }
        }


