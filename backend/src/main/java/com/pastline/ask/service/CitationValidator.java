package com.pastline.ask.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Пост-валідація відповіді: речення лишається, лише якщо всі його посилання [E#] є серед записів контексту. */
public final class CitationValidator {

    /** [E1] або [E1, E3]. */
    private static final Pattern CITATION = Pattern.compile("\\[\\s*(E\\d+(?:\\s*,\\s*E\\d+)*)\\s*]");

    private static final Pattern REF = Pattern.compile("E\\d+");
    /** Межа речення: пробіл після .!?… (якщо далі не посилання), пробіл після посилання за кінцем речення, перенос рядка. */
    private static final Pattern SENTENCE_BREAK =
            Pattern.compile("(?<=[.!?…])\\s+(?!\\[\\s*E\\d)|(?<=[.!?…]\\s{0,3}\\[[E\\d,\\s]{1,40}])\\s+|\\R+");

    private CitationValidator() {}

    /**
     * @param keptSentences речення, що пройшли перевірку
     * @param citedRefs посилання з цих речень, у порядку появи
     */
    public record Result(String answer, List<String> keptSentences, Set<String> citedRefs, int removedSentences) {}

    public static Result validate(String text, Set<String> allowedRefs) {
        List<String> kept = new ArrayList<>();
        Set<String> cited = new LinkedHashSet<>();
        int removed = 0;
        for (String sentence : sentences(text)) {
            List<String> refs = refs(sentence);
            if (!refs.isEmpty() && allowedRefs.containsAll(refs)) {
                kept.add(sentence);
                cited.addAll(refs);
            } else {
                removed++;
            }
        }
        return new Result(String.join(" ", kept), List.copyOf(kept), Set.copyOf(cited), removed);
    }

    /** Фрагмент лише з посилань ("Бігав у парку. [E1]") приєднується до попереднього речення. */
    static List<String> sentences(String text) {
        List<String> result = new ArrayList<>();
        for (String part : SENTENCE_BREAK.split(text == null ? "" : text)) {
            String s = part.strip();
            if (s.isEmpty()) {
                continue;
            }
            boolean onlyCitations = CITATION.matcher(s).replaceAll("").isBlank();
            if (onlyCitations && !result.isEmpty()) {
                result.set(result.size() - 1, result.getLast() + " " + s);
            } else {
                result.add(s);
            }
        }
        return result;
    }

    /** Усі посилання [E#] у тексті, з повтореннями, у порядку появи. */
    public static List<String> refsIn(String text) {
        return refs(text == null ? "" : text);
    }

    private static List<String> refs(String sentence) {
        List<String> refs = new ArrayList<>();
        Matcher citation = CITATION.matcher(sentence);
        while (citation.find()) {
            Matcher ref = REF.matcher(citation.group(1));
            while (ref.find()) {
                refs.add(ref.group());
            }
        }
        return refs;
    }
}
