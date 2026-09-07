package com.mediawebapp.dto;

import java.util.UUID;

public record MediaRequest(
		String title,
		String description,
		Short releaseYear,
		UUID mediaTypeId
) {
}
