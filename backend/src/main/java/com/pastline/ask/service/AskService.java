package com.pastline.ask.service;

import com.pastline.ai.port.ChatModelPort;
import com.pastline.ask.dto.AskResponse;
import com.pastline.ask.dto.AskSource;
import com.pastline.ask.dto.AskStatus;
import com.pastline.journal.entity.JournalEntry;
import com.pastline.journal.repository.JournalEntryRepository;
import com.pastline.search.dto.SearchHit;
import com.pastline.search.service.SearchMode;
import com.pastline.search.service.SearchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Ask Pastline: гібридний пошук по записах користувача, відповідь моделі лише з цих записів, перевірка посилань.
 * Відповідь без перевіреного джерела не показується. Текст питання, записів і відповіді не логується.
 */
@Slf4j
@Service
public class AskService {

    static final String NO_EVIDENCE_MESSAGE = "У записах не знайшлося відповіді на це питання.";

    private final SearchService searchService;
    private final JournalEntryRepository entryRepository;
    private final ChatModelPort chatModel;
    private final int topK;
    private final int maxEntryChars;

    public AskService(
            SearchService searchService,
            JournalEntryRepository entryRepository,
            ChatModelPort chatModel,
            @Value("${pastline.ask.top-k:8}") int topK,
            @Value("${pastline.ask.max-entry-chars:1500}") int maxEntryChars) {
        if (topK < 1 || topK > SearchService.MAX_LIMIT) {
            throw new IllegalArgumentException("pastline.ask.top-k має бути від 1 до " + SearchService.MAX_LIMIT);
        }
        this.searchService = searchService;
        this.entryRepository = entryRepository;
        this.chatModel = chatModel;
        this.topK = topK;
        this.maxEntryChars = maxEntryChars;
    }

    public AskResult ask(UUID userId, String question) {
        long start = System.nanoTime();
        String q = question.strip();
        List<SearchHit> hits =
                searchService.search(userId, q, SearchMode.HYBRID, topK, false).hits();
        if (hits.isEmpty()) {
            // Нічого шукати в моделі: відмова без виклику
            return result(AskStatus.NO_EVIDENCE, NO_EVIDENCE_MESSAGE, List.of(), 0, start, null, List.of());
        }

        // Повний текст для контексту; userId у запиті повторно гарантує, що чужий запис не потрапить до моделі
        Map<UUID, JournalEntry> entries =
                entryRepository
                        .findByUserIdAndIdIn(
                                userId, hits.stream().map(SearchHit::id).toList())
                        .stream()
                        .collect(Collectors.toMap(JournalEntry::getId, Function.identity()));

        List<AskPrompt.ContextEntry> context = new ArrayList<>();
        List<UUID> contextIds = new ArrayList<>();
        Map<String, SearchHit> hitByRef = new HashMap<>();
        for (SearchHit hit : hits) {
            JournalEntry entry = entries.get(hit.id());
            if (entry == null) {
                continue; // видалений між пошуком і читанням
            }
            String ref = "E" + (context.size() + 1);
            context.add(new AskPrompt.ContextEntry(ref, entry.getEntryDate(), entry.getTitle(), entry.getContent()));
            hitByRef.put(ref, hit);
            contextIds.add(hit.id());
        }
        if (context.isEmpty()) {
            return result(AskStatus.NO_EVIDENCE, NO_EVIDENCE_MESSAGE, List.of(), 0, start, null, List.of());
        }

        String raw = chatModel.generate(AskPrompt.SYSTEM, AskPrompt.user(q, context, maxEntryChars));
        if (AskPrompt.isNoAnswer(raw)) {
            return result(AskStatus.NO_EVIDENCE, NO_EVIDENCE_MESSAGE, List.of(), 0, start, raw, contextIds);
        }

        CitationValidator.Result validated = CitationValidator.validate(raw, Set.copyOf(hitByRef.keySet()));
        if (validated.keptSentences().isEmpty()) {
            return result(
                    AskStatus.NO_EVIDENCE,
                    NO_EVIDENCE_MESSAGE,
                    List.of(),
                    validated.removedSentences(),
                    start,
                    raw,
                    contextIds);
        }
        List<AskSource> sources = context.stream()
                .map(AskPrompt.ContextEntry::ref)
                .filter(validated.citedRefs()::contains)
                .map(ref -> {
                    SearchHit hit = hitByRef.get(ref);
                    return new AskSource(ref, hit.id(), hit.entryDate(), hit.title(), hit.snippet());
                })
                .toList();
        return result(
                AskStatus.ANSWERED, validated.answer(), sources, validated.removedSentences(), start, raw, contextIds);
    }

    private AskResult result(
            AskStatus status,
            String answer,
            List<AskSource> sources,
            int removed,
            long start,
            String raw,
            List<UUID> contextIds) {
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        log.info(
                "Ask: статус {}, записів у контексті {}, джерел {}, відкинуто речень {}, {} мс",
                status,
                contextIds.size(),
                sources.size(),
                removed,
                tookMs);
        return new AskResult(new AskResponse(status, answer, sources, removed, tookMs), raw, List.copyOf(contextIds));
    }
}
