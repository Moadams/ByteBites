package com.moadams.authservice.service;

import com.moadams.authservice.dto.LoginRequest;
import com.moadams.authservice.dto.AuthResponse; // This is your JwtResponse structure
import com.moadams.authservice.dto.UserRegistrationRequest;
import com.moadams.authservice.enums.RoleName;
import com.moadams.authservice.model.Role;
import com.moadams.authservice.model.User;
import com.moadams.authservice.repository.RoleRepository;
import com.moadams.authservice.repository.UserRepository;
import com.moadams.authservice.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service // Marks this class as a Spring service component
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Constructor injection for all dependencies
    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional // Ensures the entire method runs within a single database transaction
    public User registerUser(UserRegistrationRequest request) {
        // Check if a user with the provided email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        // Retrieve the default role (ROLE_CUSTOMER) from the database
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_CUSTOMER not found. Please pre-populate roles."));

        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(customerRole); // Assign the default role to the new user

        // Build a new User object using Lombok's @Builder
        User newUser = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // Encode the password
                .roles(defaultRoles)
                .build();

        return userRepository.save(newUser); // Save the new user to the database
    }

    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Attempt to authenticate the user using Spring Security's AuthenticationManager
            // This will use UserDetailsServiceImpl and PasswordEncoder internally
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // If authentication is successful, retrieve the authenticated User entity.
            // Since our User model implements UserDetails, we can directly cast.
            User user = (User) authentication.getPrincipal();

            // Extract role names from the User's roles for inclusion in the JWT claims
            Set<String> roleNames = new HashSet<>();
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));

            // Generate the JWT access token
            String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roleNames);

            // Determine a primary role for the response. If multiple roles, picks the first one.
            String primaryRole = roleNames.isEmpty() ? null : roleNames.iterator().next();

            // Return the custom AuthResponse (JwtResponse) structure
            return new AuthResponse(
                    accessToken,
                    "Bearer",        // tokenType
                    null,            // refreshToken (placeholder, not implemented in this flow yet)
                    jwtUtil.getExpiration(), // expiresIn (from JwtUtil configuration)
                    primaryRole      // primaryRole
            );

        } catch (BadCredentialsException e) {
            // Catch specific authentication failure (e.g., wrong password)
            throw new BadCredentialsException("Invalid email or password.");
        }
        // Other exceptions will propagate as Internal Server Error if not caught higher up.
    }
}