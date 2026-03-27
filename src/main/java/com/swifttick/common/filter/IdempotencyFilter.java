package com.swifttick.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader("Idempotency-Key");

        // Only apply for POST requests with the header
        if (!"POST".equalsIgnoreCase(request.getMethod()) || idempotencyKey == null || idempotencyKey.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String cachedResponse = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResponse != null) {
            log.info("Idempotency hit for key: {}", idempotencyKey);
            CachedResponse cr = objectMapper.readValue(cachedResponse, CachedResponse.class);
            response.setStatus(cr.status());
            response.setContentType("application/json");
            response.getWriter().write(cr.body());
            return;
        }

        // Cache the response if successful
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
            byte[] responseBody = responseWrapper.getContentAsByteArray();
            String body = new String(responseBody);
            CachedResponse cr = new CachedResponse(responseWrapper.getStatus(), body);
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(cr), 24, TimeUnit.HOURS);
        }

        responseWrapper.copyBodyToResponse();
    }

    public record CachedResponse(int status, String body) {}
}
