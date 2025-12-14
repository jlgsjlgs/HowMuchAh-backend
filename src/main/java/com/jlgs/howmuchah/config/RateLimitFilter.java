package com.jlgs.howmuchah.config;

import com.jlgs.howmuchah.util.JwtUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final JwtUtil jwtUtil;  // ← Use your existing JwtUtil

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip if rate limiting is disabled
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user ID using JwtUtil
        UUID userId = extractUserIdFromJwt();

        if (userId == null) {
            // No authentication found - let Spring Security handle it
            filterChain.doFilter(request, response);
            return;
        }

        // Use UUID as string for bucket cache key
        String userIdKey = userId.toString();
        Bucket bucket = resolveBucket(userIdKey);

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));

        if (bucket.tryConsume(1)) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for user: {}", userId);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(rateLimitProperties.getWindowMinutes() * 60));
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }

    /**
     * Extract user ID from JWT using JwtUtil
     */
    private UUID extractUserIdFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwtUtil.extractUserId(jwt);  // ← Use your JwtUtil method
        }

        return null;
    }

    /**
     * Get or create a bucket for the given user
     */
    private Bucket resolveBucket(String userId) {
        return cache.computeIfAbsent(userId, this::newBucket);
    }

    /**
     * Create a new bucket with rate limit configuration
     */
    private Bucket newBucket(String userId) {
        Bandwidth limit = Bandwidth.classic(
                rateLimitProperties.getCapacity(),
                Refill.greedy(
                        rateLimitProperties.getCapacity(),
                        Duration.ofMinutes(rateLimitProperties.getWindowMinutes())
                )
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}