package com.bytehealers.healverse.config;

import com.bytehealers.healverse.dto.response.ApiResponse;
import com.bytehealers.healverse.exception.GlobalExceptionHandler;
import com.bytehealers.healverse.service.JwtService;
import com.bytehealers.healverse.service.MyUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final MyUserDetailsService userDetailsService;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                authenticate(authHeader.substring(7), request);
            } catch (DataAccessException | TransactionException ex) {
                // Not a bad token: answering 401 here would make the app sign the user out during an outage
                logger.error("Could not load user for bearer token: database unavailable", ex);
                writeServiceUnavailable(response);
                return;
            }
        }

        // Always continue: a missing or bad token just leaves the request unauthenticated, so
        // protected routes are rejected by the AuthenticationEntryPoint and public routes still work.
        // Exceptions thrown further down the chain must not be handled here.
        filterChain.doFilter(request, response);
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(GlobalExceptionHandler.SERVICE_UNAVAILABLE_MESSAGE)));
    }

    private void authenticate(String jwt, HttpServletRequest request) {
        try {
            // Signature and expiry are verified while the subject is extracted
            String username = jwtService.extractUsername(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null,
                            userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            logger.debug("Rejected bearer token: " + ex.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }
    }
}
