package com.chatsever.log.service;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ghi append `LogEntry` thanh JSON Lines vao file, doc + filter + paginate khi truy van.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.5, doc/04_giao_thuc_truyen_thong.md § 4.4.A
 *
 * Format file: moi dong = 1 JSON-serialized LogEntry.
 * Thread-safety: synchronized writes (single-process; nhieu instance phai dung DB thay file).
 */
@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final Path logFile;
    private final ObjectMapper mapper;
    private final Object writeLock = new Object();

    public LogService(@Value("${logging.file.name:./logs/chat_log.txt}") String logFilePath) {
        this.logFile = Paths.get(logFilePath);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ensureParentDir();
    }

    private void ensureParentDir() {
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            log.warn("Khong the tao thu muc log: {}", e.getMessage());
        }
    }

    public void log(LogEntry entry) {
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(LocalDateTime.now());
        }
        synchronized (writeLock) {
            try (BufferedWriter w = Files.newBufferedWriter(
                    logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(mapper.writeValueAsString(entry));
                w.newLine();
            } catch (IOException e) {
                log.error("Ghi log that bai", e);
            }
        }
    }

    public void logChat(String sender, String content) {
        log(new LogEntry(LocalDateTime.now(), "BROADCAST", sender, null, content));
    }

    public void logPrivate(String sender, String receiver, String content) {
        log(new LogEntry(LocalDateTime.now(), "PRIVATE", sender, receiver, content));
    }

    public void logSystem(String eventType, String content) {
        log(new LogEntry(LocalDateTime.now(), eventType, null, null, content));
    }

    public PagedResponse<LogEntry> getHistory(int page, int size, String eventType) {
        if (page < 0) page = 0;
        if (size <= 0) size = 50;
        if (size > 200) size = 200;

        List<LogEntry> all = readAll();
        List<LogEntry> filtered = (eventType == null || eventType.isBlank())
                ? all
                : all.stream().filter(e -> eventType.equals(e.getEventType())).toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<LogEntry> slice = filtered.subList(from, to);

        return PagedResponse.of(slice, page, size, filtered.size());
    }

    private List<LogEntry> readAll() {
        if (!Files.exists(logFile)) {
            return List.of();
        }
        List<LogEntry> result = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try {
                    result.add(mapper.readValue(line, LogEntry.class));
                } catch (IOException ex) {
                    log.warn("Bo qua dong log loi format: {}", line);
                }
            }
        } catch (IOException e) {
            log.error("Doc file log that bai", e);
        }
        return result;
    }
}
