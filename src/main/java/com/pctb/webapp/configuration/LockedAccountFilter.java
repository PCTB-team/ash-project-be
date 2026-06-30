package com.pctb.webapp.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LockedAccountFilter extends OncePerRequestFilter {

    private final UserRepo userRepo;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken
                && authentication.isAuthenticated()) {
            String userId = jwtAuthenticationToken.getToken().getSubject();
            boolean locked = userId != null && userRepo.findById(userId)
                    .map(user -> !user.isAccountNonLocked())
                    .orElse(false);

            if (locked) {
                ErrorCode errorCode = ErrorCode.ACCOUNT_IS_LOCKED;
                response.setStatus(errorCode.getStatusCode().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.builder()
                                .code(errorCode.getCode())
                                .message(errorCode.getMessage())
                                .build()
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
