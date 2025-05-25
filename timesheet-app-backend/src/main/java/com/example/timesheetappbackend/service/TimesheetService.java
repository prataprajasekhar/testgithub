package com.example.timesheetappbackend.service;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.TimesheetRequestDTO;
import com.example.timesheetappbackend.model.Timesheet;
import com.example.timesheetappbackend.model.TimesheetStatus;
import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.repository.TimesheetRepository;
import com.example.timesheetappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TimesheetDTO createTimesheet(TimesheetRequestDTO timesheetRequestDTO, String username) {
        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found with username: " + username));

        Timesheet timesheet = new Timesheet();
        timesheet.setDate(timesheetRequestDTO.getDate());
        timesheet.setHoursWorked(timesheetRequestDTO.getHoursWorked());
        timesheet.setDescription(timesheetRequestDTO.getDescription());
        timesheet.setEmployee(employee);
        timesheet.setStatus(TimesheetStatus.PENDING);

        Timesheet savedTimesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetDTO(savedTimesheet);
    }

    @Transactional(readOnly = true)
    public List<TimesheetDTO> getTimesheetsForEmployee(String username) {
        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found with username: " + username));

        return timesheetRepository.findByEmployee(employee).stream()
                .map(this::convertToTimesheetDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimesheetDTO updateTimesheet(Long timesheetId, TimesheetRequestDTO timesheetRequestDTO, String username) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + timesheetId));

        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found with username: " + username));

        if (!timesheet.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("User is not authorized to update this timesheet.");
        }

        if (timesheet.getStatus() != TimesheetStatus.PENDING) {
            throw new RuntimeException("Timesheet can only be updated if its status is PENDING. Current status: " + timesheet.getStatus());
        }

        timesheet.setDate(timesheetRequestDTO.getDate());
        timesheet.setHoursWorked(timesheetRequestDTO.getHoursWorked());
        timesheet.setDescription(timesheetRequestDTO.getDescription());

        Timesheet updatedTimesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetDTO(updatedTimesheet);
    }

    @Transactional
    public void deleteTimesheet(Long timesheetId, String username) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + timesheetId));

        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found with username: " + username));

        if (!timesheet.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("User is not authorized to delete this timesheet.");
        }

        if (timesheet.getStatus() != TimesheetStatus.PENDING) {
            throw new RuntimeException("Timesheet can only be deleted if its status is PENDING. Current status: " + timesheet.getStatus());
        }

        timesheetRepository.delete(timesheet);
    }

    @Transactional(readOnly = true)
    public List<TimesheetDTO> getPendingTimesheetsForManager(String managerUsername) {
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager not found with username: " + managerUsername));

        return timesheetRepository.findByEmployee_ManagerAndStatus(manager, TimesheetStatus.PENDING).stream()
                .map(this::convertToTimesheetDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimesheetDTO approveTimesheet(Long timesheetId, String managerUsername) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + timesheetId));

        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager not found with username: " + managerUsername));

        if (timesheet.getEmployee().getManager() == null || !timesheet.getEmployee().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Manager is not authorized to approve this timesheet.");
        }

        if (timesheet.getStatus() != TimesheetStatus.PENDING) {
            throw new RuntimeException("Timesheet can only be approved if its status is PENDING. Current status: " + timesheet.getStatus());
        }

        timesheet.setStatus(TimesheetStatus.APPROVED);
        Timesheet savedTimesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetDTO(savedTimesheet);
    }

    @Transactional
    public TimesheetDTO rejectTimesheet(Long timesheetId, String managerUsername) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + timesheetId));

        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager not found with username: " + managerUsername));

        if (timesheet.getEmployee().getManager() == null || !timesheet.getEmployee().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Manager is not authorized to reject this timesheet.");
        }

        if (timesheet.getStatus() != TimesheetStatus.PENDING) {
            throw new RuntimeException("Timesheet can only be rejected if its status is PENDING. Current status: " + timesheet.getStatus());
        }

        timesheet.setStatus(TimesheetStatus.REJECTED);
        Timesheet savedTimesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetDTO(savedTimesheet);
    }

    private TimesheetDTO convertToTimesheetDTO(Timesheet timesheet) {
        return new TimesheetDTO(
                timesheet.getId(),
                timesheet.getDate(),
                timesheet.getHoursWorked(),
                timesheet.getDescription(),
                timesheet.getStatus().name(),
                timesheet.getEmployee().getId(),
                timesheet.getEmployee().getUsername()
        );
    }
}
