package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrinciple implements UserDetails {

    private final User user;

    public UserPrinciple(User user) {
        this.user = user;
    }

    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("USER"));
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
