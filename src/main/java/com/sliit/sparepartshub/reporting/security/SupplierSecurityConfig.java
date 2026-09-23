package com.sliit.sparepartshub.reporting.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
public class SupplierSecurityConfig {
    private final SupplierUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ActiveSupplierFilter activeSupplierFilter;

    public SupplierSecurityConfig(SupplierUserDetailsService userDetailsService,
                                  PasswordEncoder passwordEncoder,
                                  ActiveSupplierFilter activeSupplierFilter) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.activeSupplierFilter = activeSupplierFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain supplierFilterChain(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        http.securityMatcher("/supplier-portal/**")
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/supplier-portal/login").permitAll()
                        .anyRequest().hasRole("SUPPLIER"))
                .formLogin(form -> form
                        .loginPage("/supplier-portal/login")
                        .loginProcessingUrl("/supplier-portal/login")
                        .defaultSuccessUrl("/supplier-portal/dashboard", true)
                        .failureUrl("/supplier-portal/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/supplier-portal/logout")
                        .logoutSuccessUrl("/supplier-portal/login?logout"))
                .addFilterAfter(activeSupplierFilter, SecurityContextHolderFilter.class);
        return http.build();
    }
}
