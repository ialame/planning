package com.pcagrade.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
    private final UserDetailsService userDetailsService;

    // DELETED: private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/sync/progress/stream/**").permitAll()

                        // PLANNING
                        .requestMatchers("/api/planning/**").authenticated()
                        .requestMatchers("/api/work-assignments/**").authenticated()

                        // TEAMS
                        .requestMatchers(HttpMethod.GET, "/api/teams/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v2/teams/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")

                        // SYNC
                        .requestMatchers("/api/sync/**").hasAnyRole("ADMIN", "MANAGER")

                        // EMPLOYEES
                        .requestMatchers("/api/employees/**").authenticated()

                        // ORDERS
                        .requestMatchers("/api/orders/**").authenticated()

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // DEFAULT
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // OAuth2 Resource Server for Authentik tokens
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .authenticationProvider(authenticationProvider())
                // API Key filter for Symfony sync
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // DELETED: .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new AuthentikGrantedAuthoritiesConverter());
        return converter;
    }

    static class AuthentikGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            List<String> groups = jwt.getClaimAsStringList("groups");
            if (groups == null) {
                Object groupsClaim = jwt.getClaim("groups");
                if (groupsClaim instanceof List) {
                    groups = ((List<?>) groupsClaim).stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                }
            }

            if (groups != null) {
                for (String group : groups) {
                    String role = mapGroupToRole(group);
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            }

            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return authorities;
        }

        private String mapGroupToRole(String group) {
            String normalized = group.toLowerCase().trim();

            return switch (normalized) {
                case "admins", "admin", "administrators", "authentik admins" -> "ROLE_ADMIN";
                case "managers", "manager" -> "ROLE_MANAGER";
                case "noteurs", "noteur", "graders" -> "ROLE_NOTEUR";
                case "certificateurs", "certificateur", "certifiers" -> "ROLE_CERTIFICATEUR";
                case "scanneurs", "scanneur", "scanners" -> "ROLE_SCANNEUR";
                case "emballeurs", "emballeur", "packagers" -> "ROLE_EMBALLEUR";
                default -> "ROLE_" + group.toUpperCase().replace(" ", "_");
            };
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
