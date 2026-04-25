package com.chatsever.log.controller;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.dto.PagedResponse;
import com.chatsever.log.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API cho log history.
 * Spec: doc/04_giao_thuc_truyen_thong.md § 4.4.A
 *   GET /api/logs/history?page=0&size=50&eventType=BROADCAST
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/history")
    public PagedResponse<LogEntry> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType) {
        return logService.getHistory(page, size, eventType);
    }
}
