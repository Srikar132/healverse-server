package com.bytehealers.healverse.service;

import com.bytehealers.healverse.dto.request.RegisterRequest;
import com.bytehealers.healverse.exception.BadRequestException;
import com.bytehealers.healverse.exception.ResourceNotFoundException;
import com.bytehealers.healverse.model.User;
import com.bytehealers.healverse.repo.UserProfileRepository;
import com.bytehealers.healverse.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeZoneTests {

    private static final Long USER_ID = 7L;

    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new UserService(userRepository, mock(UserProfileRepository.class), encoder,
                mock(UserProfileService.class));
    }

    @Test
    void parseAcceptsIanaRegionIdsAndTrimsWhitespace() {
        assertEquals("Asia/Kolkata", TimeZones.parse(" Asia/Kolkata ").getId());
        assertEquals("UTC", TimeZones.parse("UTC").getId());
    }

    @Test
    void parseRejectsFixedOffsetsUnknownIdsAndBlank() {
        assertThrows(BadRequestException.class, () -> TimeZones.parse("+05:30"));
        assertThrows(BadRequestException.class, () -> TimeZones.parse("Mars/Olympus"));
        assertThrows(BadRequestException.class, () -> TimeZones.parse(" "));
        assertThrows(BadRequestException.class, () -> TimeZones.parse(null));
    }

    @Test
    void newUsersDefaultToUtc() {
        assertEquals("UTC", new User().getTimeZone());
    }

    @Test
    void updateStoresTheZoneOfTheUser() {
        User user = new User();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        User updated = service.updateTimeZone(USER_ID, "Asia/Kolkata");

        assertEquals("Asia/Kolkata", updated.getTimeZone());
        verify(userRepository).save(user);
    }

    @Test
    void invalidZoneLeavesTheUserUntouched() {
        User user = new User();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> service.updateTimeZone(USER_ID, "nowhere"));

        assertEquals("UTC", user.getTimeZone());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateForUnknownUserIsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateTimeZone(USER_ID, "UTC"));
    }

    @Test
    void registrationStoresTheZoneSentByTheClient() {
        RegisterRequest request = registration("Asia/Kolkata");

        assertEquals("Asia/Kolkata", service.registerUser(request).getTimeZone());
    }

    @Test
    void registrationWithoutZoneKeepsUtcAndWithInvalidZoneIsRejected() {
        assertEquals("UTC", service.registerUser(registration(null)).getTimeZone());
        assertThrows(BadRequestException.class, () -> service.registerUser(registration("nowhere")));
    }

    private static RegisterRequest registration(String timeZone) {
        RegisterRequest.Credentials credentials = new RegisterRequest.Credentials();
        credentials.setUsername("alice");
        credentials.setPassword("secret1");
        RegisterRequest request = new RegisterRequest();
        request.setUser(credentials);
        request.setTimeZone(timeZone);
        return request;
    }
}
