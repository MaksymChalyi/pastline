package lab.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Lexical {

    /** Без них "коли" із запиту збігалося б з будь-яким "коли" в записі. */
    private static final Set<String> STOP_WORDS = Set.of(
            "я", "ти", "ми", "він", "вона", "мене", "мені", "мій", "моя", "моє", "мої", "в", "у", "з", "із", "зі",
            "на", "до", "по", "за", "від", "для", "про", "під", "над", "при", "без", "через", "перед", "і", "й", "та", "а", "але", "що", "як", "коли", "чи",
            "не", "це", "був", "була", "було", "були", "би", "б", "ще", "так", "який", "яка", "які",
            "i", "me", "my", "was", "were", "is", "are", "the", "a", "an", "in", "at", "on", "to", "of",
            "and", "or", "did", "do", "does", "when", "what", "how", "it", "with", "for", "from");

    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}'’]+");
    /** Апостроф усередині слова лишається (п'ятірку), на краях прибирається. */
    private static final Pattern EDGE_APOSTROPHES = Pattern.compile("^['’]+|['’]+$");

    private Lexical() {
    }

    public static Set<String> tokens(String text) {
        return Arrays.stream(SEPARATORS.split(text.toLowerCase(Locale.ROOT)))
                .map(w -> EDGE_APOSTROPHES.matcher(w).replaceAll(""))
                .filter(w -> !w.isEmpty() && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    public static int sharedWords(Set<String> queryTokens, Set<String> entryTokens) {
        Set<String> common = new HashSet<>(queryTokens);
        common.retainAll(entryTokens);
        return common.size();
    }
}
