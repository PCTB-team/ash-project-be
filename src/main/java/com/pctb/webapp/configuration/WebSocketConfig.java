package com.pctb.webapp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final GroupChatWebSocketSecurityInterceptor groupChatWebSocketSecurityInterceptor;

    public WebSocketConfig(GroupChatWebSocketSecurityInterceptor groupChatWebSocketSecurityInterceptor) {
        this.groupChatWebSocketSecurityInterceptor = groupChatWebSocketSecurityInterceptor;
    }

    @Value("${app.cors.allowed-origins}")
    String corsAllowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:}")
    String corsAllowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(resolveAllowedOriginPatterns().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(groupChatWebSocketSecurityInterceptor);
    }

    private List<String> resolveAllowedOriginPatterns() {
        List<String> origins = parseCsv(corsAllowedOrigins);
        List<String> patterns = parseCsv(corsAllowedOriginPatterns);

        return java.util.stream.Stream.concat(origins.stream(), patterns.stream())
                .distinct()
                .toList();
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
