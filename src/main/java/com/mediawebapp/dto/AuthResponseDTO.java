package com.mediawebapp.dto;

/**
 * Login success payload. The client sends {@code token} as
 * {@code Authorization: Bearer <token>} on subsequent requests.
 */
public record AuthResponseDTO(String token) {
}
