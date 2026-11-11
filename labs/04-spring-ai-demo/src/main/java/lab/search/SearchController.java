package lab.search;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchRequest request) {
        long start = System.nanoTime();
        List<SearchHit> hits = searchService.search(request.query());
        return new SearchResponse(request.query(), (System.nanoTime() - start) / 1_000_000, hits);
    }

    public record SearchRequest(String query) {
    }

    public record SearchResponse(String query, long tookMs, List<SearchHit> hits) {
    }
}
