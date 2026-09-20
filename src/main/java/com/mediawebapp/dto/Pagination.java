package com.mediawebapp.dto;

import com.mediawebapp.exception.BadRequestException;

/**
 * Shared page/size rules for Discover and Library list endpoints.
 */
public final class Pagination {

	public static final int DEFAULT_PAGE = 0;
	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 50;

	private Pagination() {
	}

	public static void validate(int page, int size) {
		if (page < 0) {
			throw new BadRequestException("page must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new BadRequestException("size must be between 1 and " + MAX_SIZE);
		}
	}

	/** {@code page * size} as a long so large page numbers cannot overflow int. */
	public static long offset(int page, int size) {
		return (long) page * (long) size;
	}
}
