package com.procuresense.backend.web;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(OffsetDateTime timestamp,
                       int status,
                       String error,
                       String message,
                       List<String> details) {
}
