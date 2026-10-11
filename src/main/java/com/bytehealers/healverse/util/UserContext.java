package com.bytehealers.healverse.util;

import com.bytehealers.healverse.service.UserPrinciple;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrinciple principal) {
            return principal.getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
