package com.pastline.benchmark;

import java.util.List;

/**
 * Питання для оцінки Ask Pastline (H5).
 *
 * @param expect "answer": у корпусі є відповідь (targets обов'язкові); "refuse": відповіді немає, очікується відмова
 * @param targets цільові речення з targets.json; еквіваленти іншою мовою додаються при завантаженні
 */
public record AskQuestion(String id, String source, String question, String expect, List<String> targets) {

    public static final String ANSWER = "answer";
    public static final String REFUSE = "refuse";
}
