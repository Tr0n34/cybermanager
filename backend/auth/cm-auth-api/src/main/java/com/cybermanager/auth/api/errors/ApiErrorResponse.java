package com.cybermanager.auth.api.errors;

import java.time.Instant;

public record ApiErrorResponse(String message, Instant timestamp) {
}

