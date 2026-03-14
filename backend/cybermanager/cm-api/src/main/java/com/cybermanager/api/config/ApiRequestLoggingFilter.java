package com.cybermanager.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final int MAX_PAYLOAD_LENGTH = 1_500;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        String correlationId = resolveCorrelationId(request);
        long startedAt = System.currentTimeMillis();

        MDC.put("correlationId", correlationId);
        wrappedResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        LOGGER.info(
                "API request started correlationId={} method={} path={} query={}",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                sanitize(request.getQueryString())
        );

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            logCompletion(correlationId, wrappedRequest, wrappedResponse, durationMs);
            logPayloads(correlationId, wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
            MDC.remove("correlationId");
        }
    }

    private void logCompletion(String correlationId, HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        String message = "API request completed correlationId={} method={} path={} status={} durationMs={}";

        if (status >= 500) {
            LOGGER.error(message, correlationId, request.getMethod(), request.getRequestURI(), status, durationMs);
            return;
        }
        if (status >= 400) {
            LOGGER.warn(message, correlationId, request.getMethod(), request.getRequestURI(), status, durationMs);
            return;
        }
        LOGGER.info(message, correlationId, request.getMethod(), request.getRequestURI(), status, durationMs);
    }

    private void logPayloads(String correlationId, ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        LOGGER.debug(
                "API request payload correlationId={} body={}",
                correlationId,
                extractPayload(request.getContentAsByteArray(), request.getCharacterEncoding(), request.getContentType())
        );
        LOGGER.debug(
                "API response payload correlationId={} body={}",
                correlationId,
                extractPayload(response.getContentAsByteArray(), response.getCharacterEncoding(), response.getContentType())
        );
    }

    private String extractPayload(byte[] body, String encoding, String contentType) {
        if (body == null || body.length == 0) {
            return "<empty>";
        }
        if (contentType == null || (!contentType.contains("json") && !contentType.startsWith("text/"))) {
            return "<non-textual>";
        }

        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        String payload = new String(body, charset);
        String sanitized = sanitize(payload);
        if (sanitized.length() <= MAX_PAYLOAD_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_PAYLOAD_LENGTH) + "...";
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value
                .replaceAll("(?i)(\"(?:password|token|authorization|cookie|secret)\"\\s*:\\s*\")([^\"]+)(\")", "$1[REDACTED]$3")
                .replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9\\-._~+/=]+", "$1[REDACTED]");
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String existing = request.getHeader(CORRELATION_ID_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        return UUID.randomUUID().toString();
    }
}
