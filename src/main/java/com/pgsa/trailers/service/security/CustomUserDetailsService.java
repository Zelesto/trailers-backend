package com.pgsa.trailers.service.security;

import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // ADD THIS ANNOTATION
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        AppUser appUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.debug("Found user: {}, enabled: {}", appUser.getEmail(), appUser.isEnabled());

        // Force initialization of roles within transaction
        var authorities = appUser.getRoles().stream()
                .map(role -> {
                    // Force initialization of permissions if needed
                    role.getPermissions().size(); // This triggers lazy loading
                    return new SimpleGrantedAuthority("ROLE_" + role.getName());
                })
                .collect(Collectors.toList());

        log.debug("User authorities: {}", authorities);

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .authorities(authorities)
                .disabled(!appUser.isEnabled())
                .build();
    }
}