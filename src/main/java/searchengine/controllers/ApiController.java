package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.IndexingResponse;
import searchengine.dto.Response;
import searchengine.dto.ResponseSearch;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingPage;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final IndexingPage indexingPage;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingPage.indexPage(url));
    }

      @GetMapping("/search")
       public ResponseEntity<ResponseSearch> search(@RequestParam String query,
                                                    @RequestParam(required = false) String site,
                                                    @RequestParam(defaultValue = "0") Integer offset,
                                                    @RequestParam(defaultValue = "20") Integer limit){
      return ResponseEntity.ok(searchService.systemSearch(query, site, offset, limit));
     }
}
