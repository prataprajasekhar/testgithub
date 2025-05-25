package com.example.timesheetappbackend.service;

import com.example.timesheetappbackend.dto.AuthResponse;
import com.example.timesheetappbackend.dto.LoginRequest;
import com.example.timesheetappbackend.dto.RegisterRequest;
import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.repository.UserRepository;
import com.example.timesheetappbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        String role = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));

        String jwt = jwtUtil.generateToken(userDetails.getUsername(), role);
        return new AuthResponse(jwt, userDetails.getUsername(), role);
    }

    public UserDTO register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        try {
            UserRole role = UserRole.valueOf(registerRequest.getRole().toUpperCase());
            user.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error: Invalid role specified. Valid roles are ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_SUPER_ADMIN.");
        }
        
        user.setEnabled(true); // By default, new users are enabled. Can be changed by admin.

        User savedUser = userRepository.save(user);
        
        UserDTO userDTO = new UserDTO();
        userDTO.setId(savedUser.getId());
        userDTO.setUsername(savedUser.getUsername());
        userDTO.setEmail(savedUser.getEmail());
        userDTO.setRole(savedUser.getRole());
        userDTO.setEnabled(savedUser.isEnabled());
        // Manager will be null for a new registration through this basic endpoint
        userDTO.setManagerId(null);
        userDTO.setManagerName(null);
        
        return userDTO;
    }
}
