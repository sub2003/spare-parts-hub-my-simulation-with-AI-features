package com.sliit.sparepartshub.security;

import com.sliit.sparepartshub.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Re-checks the persisted staff-account active flag on every authenticated
 * staff request. This closes the gap where a user could otherwise keep using
 * an already-authenticated HTTP session after an administrator disabled the
 * account.
 */
@Component
public class ActiveStaffFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public ActiveStaffFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            boolean active = users.findById(principal.getUser().getUserId())
                    .map(user -> user.isActive())
                    .orElse(false);

            if (!active) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?disabled");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
