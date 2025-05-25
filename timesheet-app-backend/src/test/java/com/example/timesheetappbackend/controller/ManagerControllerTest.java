package com.example.timesheetappbackend.controller;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.model.TimesheetStatus;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.service.TimesheetService;
import com.example.timesheetappbackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimesheetService timesheetService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private TimesheetDTO pendingTimesheetDTO;
    private UserDTO managedEmployeeDTO;

    @BeforeEach
    void setUp() {
        pendingTimesheetDTO = new TimesheetDTO(1L, LocalDate.now(), 8, "Pending Work", TimesheetStatus.PENDING.name(), 10L, "employee");
        managedEmployeeDTO = new UserDTO(10L, "employee", "employee@example.com", UserRole.ROLE_EMPLOYEE, true, 2L, "manager");
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getPendingTimesheets_asManager_shouldReturnPendingTimesheets() throws Exception {
        List<TimesheetDTO> pendingTimesheets = Collections.singletonList(pendingTimesheetDTO);
        when(timesheetService.getPendingTimesheetsForManager("manager")).thenReturn(pendingTimesheets);

        mockMvc.perform(get("/api/manager/timesheets/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pendingTimesheetDTO.getId()))
                .andExpect(jsonPath("$[0].status").value(TimesheetStatus.PENDING.name()));
    }
    
    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getPendingTimesheets_serviceError_shouldReturnNotFound() throws Exception {
        when(timesheetService.getPendingTimesheetsForManager("manager")).thenThrow(new RuntimeException("Manager not found"));
        mockMvc.perform(get("/api/manager/timesheets/pending"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Manager not found"));
    }


    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void approveTimesheet_asManager_shouldReturnApprovedTimesheet() throws Exception {
        TimesheetDTO approvedTimesheetDTO = new TimesheetDTO(1L, LocalDate.now(), 8, "Approved Work", TimesheetStatus.APPROVED.name(), 10L, "employee");
        when(timesheetService.approveTimesheet(1L, "manager")).thenReturn(approvedTimesheetDTO);

        mockMvc.perform(put("/api/manager/timesheets/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value(TimesheetStatus.APPROVED.name()));
    }
    
    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void approveTimesheet_timesheetNotFound_shouldReturnNotFound() throws Exception {
        when(timesheetService.approveTimesheet(99L, "manager")).thenThrow(new RuntimeException("Timesheet not found with id: 99"));
        mockMvc.perform(put("/api/manager/timesheets/99/approve"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Timesheet not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void approveTimesheet_notAuthorized_shouldReturnForbidden() throws Exception {
        when(timesheetService.approveTimesheet(1L, "manager")).thenThrow(new RuntimeException("Manager is not authorized to approve this timesheet."));
        mockMvc.perform(put("/api/manager/timesheets/1/approve"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Manager is not authorized to approve this timesheet."));
    }


    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void rejectTimesheet_asManager_shouldReturnRejectedTimesheet() throws Exception {
        TimesheetDTO rejectedTimesheetDTO = new TimesheetDTO(1L, LocalDate.now(), 8, "Rejected Work", TimesheetStatus.REJECTED.name(), 10L, "employee");
        when(timesheetService.rejectTimesheet(1L, "manager")).thenReturn(rejectedTimesheetDTO);

        mockMvc.perform(put("/api/manager/timesheets/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value(TimesheetStatus.REJECTED.name()));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getManagedEmployees_asManager_shouldReturnEmployeeList() throws Exception {
        List<UserDTO> employees = Collections.singletonList(managedEmployeeDTO);
        when(userService.getManagedEmployees("manager")).thenReturn(employees);

        mockMvc.perform(get("/api/manager/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(managedEmployeeDTO.getId()))
                .andExpect(jsonPath("$[0].username").value(managedEmployeeDTO.getUsername()));
    }
    
    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getManagedEmployees_serviceError_shouldReturnNotFound() throws Exception {
        when(userService.getManagedEmployees("manager")).thenThrow(new RuntimeException("Manager not found"));
        mockMvc.perform(get("/api/manager/employees"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Manager not found"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE") // Non-manager user
    void getPendingTimesheets_asEmployee_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/manager/timesheets/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPendingTimesheets_asAnonymous_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/manager/timesheets/pending"))
                .andExpect(status().isUnauthorized());
    }
}
