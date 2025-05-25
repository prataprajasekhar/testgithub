package com.example.timesheetappbackend.dto;

import com.example.timesheetappbackend.model.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String role; // Will be converted to UserRole enum in the service
}
