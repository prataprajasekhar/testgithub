package com.example.timesheetappbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetDTO {
    private Long id;
    private LocalDate date;
    private double hoursWorked;
    private String description;
    private String status; // From TimesheetStatus enum
    private Long employeeId;
    private String employeeName;
}
