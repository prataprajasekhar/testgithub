package com.example.timesheetappbackend.controller;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.service.TimesheetService;
import com.example.timesheetappbackend.service.UserService;
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
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN')")
@Tag(name = "Manager Controller", description = "APIs for managers to handle timesheets and view their team (requires ROLE_MANAGER or ROLE_SUPER_ADMIN)")
public class ManagerController {

    @Autowired
    private TimesheetService timesheetService;

    @Autowired
    private UserService userService;

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

    @GetMapping("/timesheets/pending")
    public ResponseEntity<?> getPendingTimesheets() {
        try {
            String managerUsername = getCurrentUsername();
            List<TimesheetDTO> pendingTimesheets = timesheetService.getPendingTimesheetsForManager(managerUsername);
            return ResponseEntity.ok(pendingTimesheets);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/timesheets/{timesheetId}/approve")
    public ResponseEntity<?> approveTimesheet(@PathVariable Long timesheetId) {
        try {
            String managerUsername = getCurrentUsername();
            TimesheetDTO approvedTimesheet = timesheetService.approveTimesheet(timesheetId, managerUsername);
            return ResponseEntity.ok(approvedTimesheet);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("not authorized") || e.getMessage().contains("status is PENDING")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/timesheets/{timesheetId}/reject")
    public ResponseEntity<?> rejectTimesheet(@PathVariable Long timesheetId) {
        try {
            String managerUsername = getCurrentUsername();
            TimesheetDTO rejectedTimesheet = timesheetService.rejectTimesheet(timesheetId, managerUsername);
            return ResponseEntity.ok(rejectedTimesheet);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("not authorized") || e.getMessage().contains("status is PENDING")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<?> getManagedEmployees() {
        try {
            String managerUsername = getCurrentUsername();
            List<UserDTO> managedEmployees = userService.getManagedEmployees(managerUsername);
            return ResponseEntity.ok(managedEmployees);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
