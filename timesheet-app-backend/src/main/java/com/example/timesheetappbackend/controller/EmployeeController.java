package com.example.timesheetappbackend.controller;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.TimesheetRequestDTO;
import com.example.timesheetappbackend.service.TimesheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
@Tag(name = "Employee Controller", description = "APIs for employees to manage their timesheets (requires ROLE_EMPLOYEE, ROLE_MANAGER, or ROLE_SUPER_ADMIN)")
public class EmployeeController {

    @Autowired
    private TimesheetService timesheetService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    @PostMapping("/timesheets")
    public ResponseEntity<?> createTimesheet(@RequestBody TimesheetRequestDTO timesheetRequestDTO) {
        try {
            String username = getCurrentUsername();
            TimesheetDTO createdTimesheet = timesheetService.createTimesheet(timesheetRequestDTO, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTimesheet);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/timesheets")
    public ResponseEntity<?> getTimesheetsForEmployee() {
        try {
            String username = getCurrentUsername();
            List<TimesheetDTO> timesheets = timesheetService.getTimesheetsForEmployee(username);
            return ResponseEntity.ok(timesheets);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/timesheets/{timesheetId}")
    public ResponseEntity<?> updateTimesheet(@PathVariable Long timesheetId, @RequestBody TimesheetRequestDTO timesheetRequestDTO) {
        try {
            String username = getCurrentUsername();
            TimesheetDTO updatedTimesheet = timesheetService.updateTimesheet(timesheetId, timesheetRequestDTO, username);
            return ResponseEntity.ok(updatedTimesheet);
        } catch (RuntimeException e) {
            // More specific error codes can be returned based on exception type if needed
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("not authorized") || e.getMessage().contains("status is PENDING")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/timesheets/{timesheetId}")
    public ResponseEntity<?> deleteTimesheet(@PathVariable Long timesheetId) {
        try {
            String username = getCurrentUsername();
            timesheetService.deleteTimesheet(timesheetId, username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("not authorized") || e.getMessage().contains("status is PENDING")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
