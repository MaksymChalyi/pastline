package com.pastline.benchmark;

import java.util.List;

/**
 * Запит із набору. targets: id цільових речень корпусу (T001...), еквіваленти іншою мовою вже додані.
 *
 * @param source назва файлу без .json: хто склав запит
 * @param type exact, wordform, paraphrase або crosslingual
 */
public record BenchmarkQuery(String id, String source, String type, String lang, String text, List<String> targets) {}
