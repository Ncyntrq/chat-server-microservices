package com.chatsever.log.dto;

import java.util.List;

/**
 * Phan hoi pagination cho GET /api/logs/history.
 * Format theo doc/04_giao_thuc_truyen_thong.md § 4.4.A.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }
}
