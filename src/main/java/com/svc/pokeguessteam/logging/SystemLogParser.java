package com.svc.pokeguessteam.logging;

import com.svc.pokeguessteam.dto.admin.SystemLogEntryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o formato {@code [data] [NÍVEL] [módulo/função]: mensagem} do Logback da aplicação.
 */
public final class SystemLogParser {

    private static final Pattern LINE = Pattern.compile(
            "^\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\] \\[([A-Z]+)\\] \\[(.+?)\\]: (.*)$"
    );

    private SystemLogParser() {
    }

    public static List<SystemLogEntryDto> parse(String text) {
        List<SystemLogEntryDto> entries = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return entries;
        }
        String[] lines = text.split("\\R", -1);
        SystemLogEntryDto current = null;
        for (String line : lines) {
            Matcher matcher = LINE.matcher(line);
            if (matcher.matches()) {
                if (current != null) {
                    entries.add(current);
                }
                current = new SystemLogEntryDto(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4),
                        line
                );
            } else if (current != null && !line.isEmpty()) {
                current = new SystemLogEntryDto(
                        current.timestamp(),
                        current.level(),
                        current.origin(),
                        current.message() + "\n" + line,
                        current.raw() + "\n" + line
                );
            }
        }
        if (current != null) {
            entries.add(current);
        }
        return entries;
    }

    public static List<SystemLogEntryDto> filter(
            List<SystemLogEntryDto> entries,
            String level,
            String query,
            int limit
    ) {
        String levelFilter = level == null || level.isBlank() || "ALL".equalsIgnoreCase(level)
                ? "ALL"
                : level.trim().toUpperCase(Locale.ROOT);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<SystemLogEntryDto> matched = new ArrayList<>();
        for (SystemLogEntryDto entry : entries) {
            if (!"ALL".equals(levelFilter) && !levelFilter.equals(entry.level())) {
                continue;
            }
            if (!needle.isEmpty()) {
                String haystack = (entry.raw() == null ? "" : entry.raw()).toLowerCase(Locale.ROOT);
                if (!haystack.contains(needle)) {
                    continue;
                }
            }
            matched.add(entry);
        }

        int cap = Math.max(1, Math.min(limit, 2000));
        if (matched.size() <= cap) {
            return matched;
        }
        return List.copyOf(matched.subList(matched.size() - cap, matched.size()));
    }
}
