package com.pctb.webapp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${jwt.signerKey}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity, LockedAccountFilter lockedAccountFilter) throws Exception {
        httpSecurity
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/forgot-password/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/auth/google-login",
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh-token",
                                "/auth/test-new-access-token",
                                "/auth/test-token",
                                "/redis/set",
                                "/auth/otp-requests",
                                "/auth/otp-verification",
                                "/set-with-ttl",
                                "/increment").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ai/chat").authenticated()
                        .requestMatchers(HttpMethod.GET,"/redis/get").permitAll()
                        // Cho phép FE load avatar đã upload mà không cần quyền ADMIN.
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/settings/intro").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/settings/intro").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/groups/invite/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard/stats")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "SCOPE_ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/groups/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/documents/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/folders/**").hasAnyRole("USER", "ADMIN")
                        // Profile chỉ yêu cầu đăng nhập, khác với GET /user cần ADMIN.
                        .requestMatchers(HttpMethod.GET, "/user/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/user/profile").authenticated()
                        .requestMatchers("/payment/webhook/**").permitAll()
                        //(user login mới thanh toán)
                        .requestMatchers("/payment/**").authenticated()
                        .anyRequest().authenticated()


                )
                .oauth2Login(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))
                .addFilterAfter(lockedAccountFilter, BearerTokenAuthenticationFilter.class);


        return httpSecurity.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object scopeClaim = jwt.getClaim("scope");
            if (scopeClaim instanceof String scope) {
                Arrays.stream(scope.split("\\s+"))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .forEach(role -> {
                            String normalizedRole = role.toUpperCase(Locale.ROOT);
                            String roleWithoutPrefix = normalizedRole.startsWith("ROLE_")
                                    ? normalizedRole.substring("ROLE_".length())
                                    : normalizedRole;
                            authorities.add(new SimpleGrantedAuthority(roleWithoutPrefix));
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleWithoutPrefix));
                        });
            }
            return authorities;
        });
        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(parseCorsAllowedOrigins());
        configuration.setAllowedOriginPatterns(parseCorsAllowedOriginPatterns());

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    private List<String> parseCorsAllowedOrigins() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private List<String> parseCorsAllowedOriginPatterns() {
        return Arrays.stream(corsAllowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
