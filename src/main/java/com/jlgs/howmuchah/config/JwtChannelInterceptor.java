package com.jlgs.howmuchah.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT from headers
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                String token = authorization.get(0);

                // Remove "Bearer " prefix if present
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                try {
                    // Validate JWT
                    Jwt jwt = jwtDecoder.decode(token);

                    // Create authentication token
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

                    // Set user in WebSocket session
                    accessor.setUser(authentication);

                    log.info("WebSocket authenticated for user: {}", jwt.getSubject());
                } catch (JwtException e) {
                    log.error("Invalid JWT token for WebSocket connection: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            } else {
                log.error("No Authorization header found in WebSocket CONNECT frame");
                throw new IllegalArgumentException("Missing JWT token");
            }
        }

        return message;
    }
}