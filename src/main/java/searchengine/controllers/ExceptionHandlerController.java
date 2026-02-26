package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.IndexingErrorResponse;
import searchengine.dto.IndexingException;


@RestControllerAdvice

public class ExceptionHandlerController {
    @ExceptionHandler({IndexingException.class})
    public ResponseEntity<IndexingErrorResponse> badRequest(IndexingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new IndexingErrorResponse(false, e.getMessage()));
    }
}
