package com.example.timesheetappbackend.service;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.TimesheetRequestDTO;
import com.example.timesheetappbackend.model.Timesheet;
import com.example.timesheetappbackend.model.TimesheetStatus;
import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.repository.TimesheetRepository;
import com.example.timesheetappbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimesheetServiceTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TimesheetService timesheetService;

    private User employee, manager, otherManager;
    private Timesheet timesheetPending, timesheetApproved;
    private TimesheetRequestDTO timesheetRequestDTO;

    @BeforeEach
    void setUp() {
        manager = new User(1L, "manager", "pass", "manager@example.com", UserRole.ROLE_MANAGER, true, null, new HashSet<>());
        employee = new User(2L, "employee", "pass", "employee@example.com", UserRole.ROLE_EMPLOYEE, true, manager, new HashSet<>());
        manager.getManagedEmployees().add(employee);
        otherManager = new User(3L, "otherManager", "pass", "othermanager@example.com", UserRole.ROLE_MANAGER, true, null, new HashSet<>());


        timesheetRequestDTO = new TimesheetRequestDTO();
        timesheetRequestDTO.setDate(LocalDate.now());
        timesheetRequestDTO.setHoursWorked(8);
        timesheetRequestDTO.setDescription("Work done");

        timesheetPending = new Timesheet(10L, LocalDate.now(), 8, "Pending work", TimesheetStatus.PENDING, employee);
        timesheetApproved = new Timesheet(11L, LocalDate.now().minusDays(1), 8, "Approved work", TimesheetStatus.APPROVED, employee);
    }

    @Test
    void createTimesheet_successful() {
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> {
            Timesheet ts = invocation.getArgument(0);
            ts.setId(1L); // Simulate saving and getting an ID
            return ts;
        });

        TimesheetDTO createdTimesheet = timesheetService.createTimesheet(timesheetRequestDTO, employee.getUsername());

        assertNotNull(createdTimesheet);
        assertEquals(timesheetRequestDTO.getHoursWorked(), createdTimesheet.getHoursWorked());
        assertEquals(employee.getUsername(), createdTimesheet.getEmployeeName());
        assertEquals(TimesheetStatus.PENDING.name(), createdTimesheet.getStatus());
        verify(userRepository).findByUsername(employee.getUsername());
        verify(timesheetRepository).save(any(Timesheet.class));
    }
    
    @Test
    void createTimesheet_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> timesheetService.createTimesheet(timesheetRequestDTO, "unknown"));
        assertEquals("Employee not found with username: unknown", exception.getMessage());
        verify(userRepository).findByUsername("unknown");
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void getTimesheetsForEmployee_successful() {
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));
        when(timesheetRepository.findByEmployee(employee)).thenReturn(Collections.singletonList(timesheetPending));

        List<TimesheetDTO> timesheets = timesheetService.getTimesheetsForEmployee(employee.getUsername());

        assertEquals(1, timesheets.size());
        assertEquals(timesheetPending.getId(), timesheets.get(0).getId());
        verify(userRepository).findByUsername(employee.getUsername());
        verify(timesheetRepository).findByEmployee(employee);
    }
    
    @Test
    void getTimesheetsForEmployee_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> timesheetService.getTimesheetsForEmployee("unknown"));
        assertEquals("Employee not found with username: unknown", exception.getMessage());
        verify(userRepository).findByUsername("unknown");
        verify(timesheetRepository, never()).findByEmployee(any(User.class));
    }

    @Test
    void updateTimesheet_successful() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(timesheetPending);

        TimesheetRequestDTO updateDTO = new TimesheetRequestDTO();
        updateDTO.setDate(LocalDate.now().plusDays(1));
        updateDTO.setHoursWorked(7);
        updateDTO.setDescription("Updated work");

        TimesheetDTO updatedTimesheet = timesheetService.updateTimesheet(timesheetPending.getId(), updateDTO, employee.getUsername());

        assertNotNull(updatedTimesheet);
        assertEquals(updateDTO.getHoursWorked(), updatedTimesheet.getHoursWorked());
        assertEquals(updateDTO.getDescription(), updatedTimesheet.getDescription());
        verify(timesheetRepository).findById(timesheetPending.getId());
        verify(userRepository).findByUsername(employee.getUsername());
        verify(timesheetRepository).save(timesheetPending);
    }

    @Test
    void updateTimesheet_timesheetNotFound() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.updateTimesheet(99L, timesheetRequestDTO, employee.getUsername()));
        assertEquals("Timesheet not found with id: 99", exception.getMessage());
        verify(timesheetRepository).findById(99L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void updateTimesheet_notOwner() {
        User anotherEmployee = new User(5L, "another", "pass", "another@example.com", UserRole.ROLE_EMPLOYEE, true, manager, new HashSet<>());
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending)); // timesheetPending belongs to 'employee'
        when(userRepository.findByUsername(anotherEmployee.getUsername())).thenReturn(Optional.of(anotherEmployee));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.updateTimesheet(timesheetPending.getId(), timesheetRequestDTO, anotherEmployee.getUsername()));
        assertEquals("User is not authorized to update this timesheet.", exception.getMessage());
        verify(timesheetRepository).findById(timesheetPending.getId());
        verify(userRepository).findByUsername(anotherEmployee.getUsername());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void updateTimesheet_notPending() {
        when(timesheetRepository.findById(timesheetApproved.getId())).thenReturn(Optional.of(timesheetApproved)); // timesheetApproved is not PENDING
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.updateTimesheet(timesheetApproved.getId(), timesheetRequestDTO, employee.getUsername()));
        assertEquals("Timesheet can only be updated if its status is PENDING. Current status: APPROVED", exception.getMessage());
        verify(timesheetRepository).findById(timesheetApproved.getId());
        verify(userRepository).findByUsername(employee.getUsername());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void deleteTimesheet_successful() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));
        doNothing().when(timesheetRepository).delete(timesheetPending);

        assertDoesNotThrow(() -> timesheetService.deleteTimesheet(timesheetPending.getId(), employee.getUsername()));
        verify(timesheetRepository).delete(timesheetPending);
    }
    
    @Test
    void deleteTimesheet_timesheetNotFound() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.deleteTimesheet(99L, employee.getUsername()));
        assertEquals("Timesheet not found with id: 99", exception.getMessage());
        verify(timesheetRepository).findById(99L);
        verify(timesheetRepository, never()).delete(any(Timesheet.class));
    }

    @Test
    void deleteTimesheet_notOwner() {
        User anotherEmployee = new User(5L, "another", "pass", "another@example.com", UserRole.ROLE_EMPLOYEE, true, manager, new HashSet<>());
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(anotherEmployee.getUsername())).thenReturn(Optional.of(anotherEmployee));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.deleteTimesheet(timesheetPending.getId(), anotherEmployee.getUsername()));
        assertEquals("User is not authorized to delete this timesheet.", exception.getMessage());
        verify(timesheetRepository, never()).delete(any(Timesheet.class));
    }

    @Test
    void deleteTimesheet_notPending() {
        when(timesheetRepository.findById(timesheetApproved.getId())).thenReturn(Optional.of(timesheetApproved));
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.deleteTimesheet(timesheetApproved.getId(), employee.getUsername()));
        assertEquals("Timesheet can only be deleted if its status is PENDING. Current status: APPROVED", exception.getMessage());
        verify(timesheetRepository, never()).delete(any(Timesheet.class));
    }


    @Test
    void getPendingTimesheetsForManager_successful() {
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));
        when(timesheetRepository.findByEmployee_ManagerAndStatus(manager, TimesheetStatus.PENDING))
            .thenReturn(Collections.singletonList(timesheetPending));

        List<TimesheetDTO> pendingTimesheets = timesheetService.getPendingTimesheetsForManager(manager.getUsername());

        assertEquals(1, pendingTimesheets.size());
        assertEquals(timesheetPending.getId(), pendingTimesheets.get(0).getId());
        verify(userRepository).findByUsername(manager.getUsername());
        verify(timesheetRepository).findByEmployee_ManagerAndStatus(manager, TimesheetStatus.PENDING);
    }
    
    @Test
    void getPendingTimesheetsForManager_managerNotFound() {
        when(userRepository.findByUsername("unknownManager")).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.getPendingTimesheetsForManager("unknownManager"));
        assertEquals("Manager not found with username: unknownManager", exception.getMessage());
        verify(userRepository).findByUsername("unknownManager");
        verify(timesheetRepository, never()).findByEmployee_ManagerAndStatus(any(User.class), any(TimesheetStatus.class));
    }


    @Test
    void approveTimesheet_successful() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimesheetDTO approvedDTO = timesheetService.approveTimesheet(timesheetPending.getId(), manager.getUsername());

        assertEquals(TimesheetStatus.APPROVED.name(), approvedDTO.getStatus());
        verify(timesheetRepository).save(timesheetPending);
    }
    
    @Test
    void approveTimesheet_timesheetNotFound() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager)); // Manager lookup might happen before timesheet
        
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.approveTimesheet(99L, manager.getUsername()));
        assertEquals("Timesheet not found with id: 99", exception.getMessage());
        verify(timesheetRepository).findById(99L);
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void approveTimesheet_notManagerOfEmployee() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(otherManager.getUsername())).thenReturn(Optional.of(otherManager)); // 'otherManager' is not manager of 'employee'

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.approveTimesheet(timesheetPending.getId(), otherManager.getUsername()));
        assertEquals("Manager is not authorized to approve this timesheet.", exception.getMessage());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void approveTimesheet_notPending() {
        when(timesheetRepository.findById(timesheetApproved.getId())).thenReturn(Optional.of(timesheetApproved));
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.approveTimesheet(timesheetApproved.getId(), manager.getUsername()));
        assertEquals("Timesheet can only be approved if its status is PENDING. Current status: APPROVED", exception.getMessage());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void rejectTimesheet_successful() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimesheetDTO rejectedDTO = timesheetService.rejectTimesheet(timesheetPending.getId(), manager.getUsername());

        assertEquals(TimesheetStatus.REJECTED.name(), rejectedDTO.getStatus());
        verify(timesheetRepository).save(timesheetPending);
    }
    
    @Test
    void rejectTimesheet_timesheetNotFound() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.rejectTimesheet(99L, manager.getUsername()));
        assertEquals("Timesheet not found with id: 99", exception.getMessage());
         verify(timesheetRepository).findById(99L);
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void rejectTimesheet_notManagerOfEmployee() {
        when(timesheetRepository.findById(timesheetPending.getId())).thenReturn(Optional.of(timesheetPending));
        when(userRepository.findByUsername(otherManager.getUsername())).thenReturn(Optional.of(otherManager));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.rejectTimesheet(timesheetPending.getId(), otherManager.getUsername()));
        assertEquals("Manager is not authorized to reject this timesheet.", exception.getMessage());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void rejectTimesheet_notPending() {
        when(timesheetRepository.findById(timesheetApproved.getId())).thenReturn(Optional.of(timesheetApproved));
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> timesheetService.rejectTimesheet(timesheetApproved.getId(), manager.getUsername()));
        assertEquals("Timesheet can only be rejected if its status is PENDING. Current status: APPROVED", exception.getMessage());
        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }
}
