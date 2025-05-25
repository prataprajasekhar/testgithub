package com.example.timesheetappbackend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TimesheetRequestDTO {
    private LocalDate date;
    private double hoursWorked;
    private String description;
}
