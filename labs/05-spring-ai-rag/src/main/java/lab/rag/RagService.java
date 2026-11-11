package lab.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final int TOP_K = 5;
    private static final Pattern CITATION = Pattern.compile("\\[(E\\d+)]");
    private static final String THINK_END = "</think>";
    private static final String NO_ANSWER = "У записах цього немає";

    private static final String PROMPT = """
            Відповідай ТІЛЬКИ на основі записів нижче. Якщо відповіді немає, скажи "%s".
            Після кожного твердження вкажи [id запису]. Відповідай українською.

            %s
            Питання: %s
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    public List<Source> search(String query) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(TOP_K).build())
                .stream().map(Source::from).toList();
    }

    public ManualAnswer askManual(String question) {
        List<Source> sources = search(question);
        String context = sources.stream()
                .map(s -> "<entry id=\"%s\" date=\"%s\">%s</entry>".formatted(s.id(), s.date(), s.text()))
                .collect(Collectors.joining("\n"));

        String answer = stripThinking(chatClient.prompt()
                .user(PROMPT.formatted(NO_ANSWER, context, question))
                .options(OllamaChatOptions.builder().disableThinking())
                .call()
                .content());

        Set<String> found = sources.stream().map(Source::id).collect(Collectors.toSet());
        Set<String> cited = citedIds(answer);
        Set<String> unknown = cited.stream().filter(id -> !found.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ManualAnswer(answer, status(answer, cited, unknown), cited, unknown, sources);
    }

    /** Відповідь без жодного [id] (і не відмова) теж неперевірна. */
    static String status(String answer, Set<String> cited, Set<String> unknown) {
        boolean refusal = answer.contains(NO_ANSWER);
        return unknown.isEmpty() && (refusal || !cited.isEmpty()) ? "VERIFIED" : "UNVERIFIED";
    }

    /** Пошук і промпт будує Spring AI; SimpleLoggerAdvisor пише промпт у лог. */
    public String askWithAdvisor(String question) {
        QuestionAnswerAdvisor qa = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(TOP_K).build())
                .build();
        return stripThinking(chatClient.prompt()
                .advisors(qa, new SimpleLoggerAdvisor())
                .user(question)
                .options(OllamaChatOptions.builder().disableThinking())
                .call()
                .content());
    }

    /** qwen3:4b пише роздуми в content навіть з disableThinking() (лаба 04). */
    static String stripThinking(String content) {
        if (content == null) {
            return "";
        }
        int end = content.lastIndexOf(THINK_END);
        return (end >= 0 ? content.substring(end + THINK_END.length()) : content).strip();
    }

    static Set<String> citedIds(String answer) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(answer);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    public record ManualAnswer(String answer, String status, Set<String> citedIds, Set<String> unknownIds,
                               List<Source> sources) {
    }
}
