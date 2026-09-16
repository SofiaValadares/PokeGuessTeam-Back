package com.svc.pokeguessteam.logging;

import com.svc.pokeguessteam.dto.admin.SystemLogEntryDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemLogParserTest {

    @Test
    void parseAndFilterKeepsStackTraceOnPreviousEntry() {
        String text = """
                [2026-09-09 18:00:00.001] [INFO] [AuthController/login]: Sessão autenticada userId=1
                [2026-09-09 18:00:01.002] [ERROR] [GlobalExceptionHandler/handleUnexpected]: Erro não tratado
                java.lang.IllegalStateException: boom
                	at com.example.Foo.bar(Foo.java:10)
                [2026-09-09 18:00:02.003] [WARN] [RequestLoggingInterceptor/afterCompletion]: GET /x -> 400
                """;

        List<SystemLogEntryDto> parsed = SystemLogParser.parse(text);
        assertEquals(3, parsed.size());
        assertTrue(parsed.get(1).message().contains("IllegalStateException"));
        assertTrue(parsed.get(1).raw().contains("Foo.java:10"));

        List<SystemLogEntryDto> errors = SystemLogParser.filter(parsed, "ERROR", "", 50);
        assertEquals(1, errors.size());

        List<SystemLogEntryDto> search = SystemLogParser.filter(parsed, "ALL", "userId=1", 50);
        assertEquals(1, search.size());
        assertEquals("INFO", search.get(0).level());
    }
}
