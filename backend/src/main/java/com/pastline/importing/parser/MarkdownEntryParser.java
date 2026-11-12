package com.pastline.importing.parser;

import com.pastline.journal.dto.EntryRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Формат: кожен запис починається із заголовка другого рівня з датою, далі текст до наступного такого заголовка.
 *
 * <pre>
 * ## 2026-11-01 Необов'язковий заголовок
 * Текст запису...
 * </pre>
 *
 * Заголовки інших рівнів (#, ###) лишаються частиною тексту.
 */
@Component
public class MarkdownEntryParser {

    private static final Pattern HEADING = Pattern.compile("^##\\s+(.*)$");
    private static final Pattern DATE_AND_TITLE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})(?:\\s+(.*))?$");

    public List<ParsedItem> parse(String text) {
        List<ParsedItem> items = new ArrayList<>();
        String heading = null;
        StringBuilder body = new StringBuilder();
        boolean textBeforeFirstHeading = false;

        for (String line : text.replace("\r\n", "\n").split("\n", -1)) {
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                if (heading != null) {
                    items.add(toItem(heading, body.toString()));
                }
                heading = m.group(1).strip();
                body.setLength(0);
            } else if (heading != null) {
                body.append(line).append('\n');
            } else if (!line.isBlank()) {
                textBeforeFirstHeading = true;
            }
        }
        if (heading != null) {
            items.add(toItem(heading, body.toString()));
        }
        if (textBeforeFirstHeading) {
            // Не губимо текст мовчки: користувач має побачити, що він не імпортований
            items.addFirst(ParsedItem.failed("Текст перед першим заголовком \"## yyyy-MM-dd\" не імпортовано"));
        }
        return items;
    }

    private static ParsedItem toItem(String heading, String body) {
        Matcher m = DATE_AND_TITLE.matcher(heading);
        if (!m.matches()) {
            return ParsedItem.failed("Заголовок запису має починатися з дати yyyy-MM-dd");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(m.group(1));
        } catch (DateTimeParseException e) {
            return ParsedItem.failed("Некоректна дата в заголовку");
        }
        return ParsedItem.ok(new EntryRequest(m.group(2), body.strip(), date));
    }
}
