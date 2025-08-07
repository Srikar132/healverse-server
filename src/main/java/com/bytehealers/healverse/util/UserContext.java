package com.bytehealers.healverse.util;

import com.bytehealers.healverse.service.UserPrinciple;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

    public Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrinciple) {
            return ((UserPrinciple) principal).getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }

    public String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrinciple) {
            return ((UserPrinciple) principal).getUsername();
        }
        throw new RuntimeException("User not authenticated");
    }
}
