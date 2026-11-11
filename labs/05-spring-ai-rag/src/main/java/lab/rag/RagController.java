package lab.rag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService rag;

    public RagController(RagService rag) {
        this.rag = rag;
    }

    @GetMapping("/search")
    public Timed<List<Source>> search(@RequestParam String q) {
        return timed(() -> rag.search(q));
    }

    @GetMapping("/ask-manual")
    public Timed<RagService.ManualAnswer> askManual(@RequestParam String q) {
        return timed(() -> rag.askManual(q));
    }

    @GetMapping("/ask")
    public Timed<String> ask(@RequestParam String q) {
        return timed(() -> rag.askWithAdvisor(q));
    }

    private static <T> Timed<T> timed(Supplier<T> action) {
        long start = System.nanoTime();
        T result = action.get();
        return new Timed<>((System.nanoTime() - start) / 1_000_000, result);
    }

    public record Timed<T>(long tookMs, T result) {
    }
}
