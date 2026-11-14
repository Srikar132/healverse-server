package com.bytehealers.healverse.dto.request;

import com.bytehealers.healverse.dto.UserProfileDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registration payload. Deliberately not the {@code User} / {@code UserProfile} entities, so a client
 * cannot set ids, timestamps or the Google binding.
 */
@Data
public class RegisterRequest {

    @NotNull(message = "User details are required")
    @Valid
    private Credentials user;

    @Valid
    private UserProfileDTO profile;

    @Data
    public static class Credentials {

        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
        private String username;

        // 72 is bcrypt's input limit
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
        private String password;

        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        private String email;
    }
}
