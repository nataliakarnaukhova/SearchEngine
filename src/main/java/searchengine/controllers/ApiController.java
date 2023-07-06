package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.Response;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    @GetMapping("/statistics")
    public ResponseEntity<Response> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexingOneSite(@RequestParam(name = "url", defaultValue = "") String url) {
        return ResponseEntity.ok(indexingService.startIndexingOneSite(url));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam(name = "query") String query,
                                           @RequestParam(name = "site", required = false) String url,
                                           @RequestParam(name = "offset",
                                                   required = false, defaultValue = "0") Integer offset,
                                           @RequestParam(name = "limit",
                                                   required = false, defaultValue = "20") Integer limit) {
        return ResponseEntity.ok(searchingService.searchText(query, url, offset, limit));
    }
}
