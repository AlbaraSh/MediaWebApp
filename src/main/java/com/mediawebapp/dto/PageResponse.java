package com.mediawebapp.dto;

import java.util.List;

/**
 * Page envelope for list endpoints. Controllers serialize this record rather
 * than {@code org.springframework.data.domain.Page}.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
		return new PageResponse<>(List.copyOf(content), page, size, totalElements, totalPages);
	}
}
