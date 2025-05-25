package com.example.timesheetappbackend.dto;

import com.example.timesheetappbackend.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private boolean enabled;
    private Long managerId;
    private String managerName;
}
