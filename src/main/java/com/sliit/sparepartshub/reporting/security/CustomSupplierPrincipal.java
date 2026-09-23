package com.sliit.sparepartshub.reporting.security;

import com.sliit.sparepartshub.entity.Supplier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts the Supplier entity to Spring Security's UserDetails contract,
 * mirroring CustomUserPrincipal for staff. Kept as a separate class (not
 * a shared one) because Supplier and User are unrelated entities with no
 * common interface - see SupplierSecurityConfig for why this needs its
 * own filter chain entirely.
 */
public class CustomSupplierPrincipal implements UserDetails {

    private final Supplier supplier;

    public CustomSupplierPrincipal(Supplier supplier) {
        this.supplier = supplier;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"));
    }

    @Override
    public String getPassword() {
        return supplier.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return supplier.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return supplier.isActive();
    }
}
