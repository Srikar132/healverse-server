package com.bytehealers.healverse.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated user's id into a controller method parameter:
 * {@code public ... endpoint(@CurrentUserId Long userId)}.
 * Resolved by Spring Security from {@link com.bytehealers.healverse.service.UserPrinciple#getUserId()}, so
 * controllers need no helper bean and can be tested without a SecurityContext.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "userId")
public @interface CurrentUserId {
}
