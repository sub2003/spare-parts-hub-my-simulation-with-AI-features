package com.sliit.sparepartshub.reporting.security;

import com.sliit.sparepartshub.reporting.repository.SupplierRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ActiveSupplierFilter extends OncePerRequestFilter {
    private final SupplierRepository suppliers;

    public ActiveSupplierFilter(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomSupplierPrincipal principal) {
            boolean active = suppliers.findById(principal.getSupplier().getSupplierId())
                    .map(s -> s.isActive())
                    .orElse(false);
            if (!active) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                response.sendRedirect(request.getContextPath() + "/supplier-portal/login?inactive");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
