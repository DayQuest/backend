package com.dayquest.dayquestbackend;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Collections;

@Configuration
public class SecurityConfiguration {

    private final SecurityContextRepository repository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(SecurityContextRepository repository, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.repository = repository;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/api/users/auth",
                                "/api/users/login",
                                "/api/users/register",
                                "/swagger-ui/**",
                                "/v3/**",
                                "/api/users/verify",
                                "/api/users/resendcode",
                                "/api/beta/new-key",
                                "/api/beta/get-key",
                                "/api/beta/remove-key",
                                "/api/users/profilepicture/**",
                                "/api/videos/thumbnail/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .securityContext(request -> request.securityContextRepository(repository))
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint((request, response, authException) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.getWriter().write("Authentication failed: " + authException.getMessage());
                                })
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                    response.getWriter().write("Access denied: " + accessDeniedException.getMessage());
                                })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByEmail(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with email: " + username);
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities(Collections.emptyList())
                    .build();
        };
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}

