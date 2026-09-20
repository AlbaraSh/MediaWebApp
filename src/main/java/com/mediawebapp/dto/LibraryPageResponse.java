package com.mediawebapp.dto;

import java.util.List;

/**
 * Library list page: same envelope fields as {@link PageResponse} plus status counts.
 */
public record LibraryPageResponse(
		List<UserMediaResponseDTO> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		UserMediaStatusCounts counts
) {

	public static LibraryPageResponse of(
			PageResponse<UserMediaResponseDTO> page,
			UserMediaStatusCounts counts) {
		return new LibraryPageResponse(
				page.content(),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages(),
				counts);
	}
}
