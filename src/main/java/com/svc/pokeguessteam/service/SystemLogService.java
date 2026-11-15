package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.admin.SystemLogEntryDto;
import com.svc.pokeguessteam.dto.admin.SystemLogListResponse;
import com.svc.pokeguessteam.logging.AppLogger;
import com.svc.pokeguessteam.logging.SystemLogParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
public class SystemLogService {

    private static final AppLogger log = AppLogger.create(SystemLogService.class);
    private static final int MAX_TAIL_BYTES = 1_048_576;

    private final AdminAccessService adminAccessService;
    private final Path logFile;

    public SystemLogService(
            AdminAccessService adminAccessService,
            @Value("${app.logs.file:logs/pokeguessteam.log}") String logFilePath
    ) {
        this.adminAccessService = adminAccessService;
        this.logFile = Path.of(logFilePath);
    }

    public SystemLogListResponse readLogs(String adminId, String level, String query, int limit) {
        adminAccessService.requireAdmin(adminId);
        int safeLimit = limit <= 0 ? 500 : Math.min(limit, 2000);

        if (!Files.isRegularFile(logFile)) {
            log.warn("readLogs", "Ficheiro de log não encontrado: {}", logFile.toAbsolutePath());
            return new SystemLogListResponse(List.of(), 0, false);
        }

        try {
            String tail = readTail(logFile, MAX_TAIL_BYTES);
            List<SystemLogEntryDto> parsed = SystemLogParser.parse(tail);
            boolean truncated = tail.length() >= MAX_TAIL_BYTES || parsed.size() > safeLimit;
            List<SystemLogEntryDto> filtered = SystemLogParser.filter(parsed, level, query, safeLimit);
            log.info("readLogs", "Logs lidos count={} level={}", filtered.size(), level);
            return new SystemLogListResponse(filtered, filtered.size(), truncated);
        } catch (IOException ex) {
            log.error("readLogs", "Falha a ler ficheiro de log {}", ex, logFile.toAbsolutePath());
            throw new IllegalStateException("Não foi possível ler o ficheiro de logs.");
        }
    }

    static String readTail(Path path, int maxBytes) throws IOException {
        long size = Files.size(path);
        if (size <= 0) {
            return "";
        }
        long start = Math.max(0, size - maxBytes);
        int length = (int) (size - start);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        try (var channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            channel.position(start);
            channel.read(buffer);
        }
        String text = new String(buffer.array(), StandardCharsets.UTF_8);
        if (start > 0) {
            int newline = text.indexOf('\n');
            if (newline >= 0 && newline + 1 < text.length()) {
                return text.substring(newline + 1);
            }
        }
        return text;
    }
}
