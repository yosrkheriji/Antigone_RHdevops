package com.antigone.rh.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("Utilisateur authentifié introuvable");
        }
        return principal;
    }

    public static Long requireEmployeId() {
        AuthPrincipal principal = getCurrentPrincipal();
        if (principal.getEmployeId() == null) {
            throw new IllegalStateException("Compte employé requis");
        }
        return principal.getEmployeId();
    }
}