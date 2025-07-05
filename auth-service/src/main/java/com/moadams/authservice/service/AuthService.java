package com.moadams.authservice.service;

import com.moadams.authservice.dto.LoginRequest;
import com.moadams.authservice.dto.AuthResponse;
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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


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

    @Transactional
    public User registerUser(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }


        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_CUSTOMER not found. Please pre-populate roles."));

        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(customerRole);


        User newUser = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(defaultRoles)
                .build();

        return userRepository.save(newUser);
    }

    @Transactional
    public User registerRestaurantOwner(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }


        Role customerRole = roleRepository.findByName(RoleName.ROLE_RESTAURANT_OWNER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_RESTAURANT_OWNER not found. Please pre-populate roles."));

        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(customerRole);


        User newUser = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(defaultRoles)
                .build();

        return userRepository.save(newUser);
    }

    public AuthResponse loginUser(LoginRequest request) {
        try {


            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );



            User user = (User) authentication.getPrincipal();


            Set<String> roleNames = new HashSet<>();
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));


            String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roleNames);


            String primaryRole = roleNames.isEmpty() ? null : roleNames.iterator().next();


            return new AuthResponse(
                    accessToken,
                    "Bearer",
                    null,
                    jwtUtil.getExpiration(),
                    primaryRole
            );

        } catch (BadCredentialsException e) {

            throw new BadCredentialsException("Invalid email or password.");
        }

    }
}