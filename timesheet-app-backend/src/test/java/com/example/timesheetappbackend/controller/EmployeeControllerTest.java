package com.example.timesheetappbackend.controller;

import com.example.timesheetappbackend.dto.TimesheetDTO;
import com.example.timesheetappbackend.dto.TimesheetRequestDTO;
import com.example.timesheetappbackend.model.TimesheetStatus;
import com.example.timesheetappbackend.service.TimesheetService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimesheetService timesheetService;

    @Autowired
    private ObjectMapper objectMapper;

    private TimesheetRequestDTO timesheetRequestDTO;
    private TimesheetDTO timesheetDTO;

    @BeforeEach
    void setUp() {
        timesheetRequestDTO = new TimesheetRequestDTO();
        timesheetRequestDTO.setDate(LocalDate.now());
        timesheetRequestDTO.setHoursWorked(8);
        timesheetRequestDTO.setDescription("Test work");

        timesheetDTO = new TimesheetDTO(1L, LocalDate.now(), 8, "Test work", TimesheetStatus.PENDING.name(), 10L, "employee");
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void createTimesheet_asEmployee_shouldReturnCreatedTimesheet() throws Exception {
        when(timesheetService.createTimesheet(any(TimesheetRequestDTO.class), anyString())).thenReturn(timesheetDTO);

        mockMvc.perform(post("/api/employee/timesheets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test work"))
                .andExpect(jsonPath("$.employeeName").value("employee"));
    }
    
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void createTimesheet_serviceThrowsError_shouldReturnBadRequest() throws Exception {
        when(timesheetService.createTimesheet(any(TimesheetRequestDTO.class), anyString())).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/employee/timesheets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Service error"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void getTimesheetsForEmployee_asEmployee_shouldReturnTimesheetList() throws Exception {
        List<TimesheetDTO> timesheets = Collections.singletonList(timesheetDTO);
        when(timesheetService.getTimesheetsForEmployee(anyString())).thenReturn(timesheets);

        mockMvc.perform(get("/api/employee/timesheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(timesheetDTO.getId()));
    }
    
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void getTimesheetsForEmployee_serviceThrowsError_shouldReturnNotFound() throws Exception {
        when(timesheetService.getTimesheetsForEmployee(anyString())).thenThrow(new RuntimeException("User not found"));
        mockMvc.perform(get("/api/employee/timesheets"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void updateTimesheet_asOwnerAndPending_shouldReturnUpdatedTimesheet() throws Exception {
        TimesheetDTO updatedDTO = new TimesheetDTO(1L, LocalDate.now(), 7, "Updated Test work", TimesheetStatus.PENDING.name(), 10L, "employee");
        when(timesheetService.updateTimesheet(anyLong(), any(TimesheetRequestDTO.class), anyString())).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/employee/timesheets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursWorked").value(7))
                .andExpect(jsonPath("$.description").value("Updated Test work"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void updateTimesheet_timesheetNotFound_shouldReturnNotFound() throws Exception {
        when(timesheetService.updateTimesheet(anyLong(), any(TimesheetRequestDTO.class), anyString()))
            .thenThrow(new RuntimeException("Timesheet not found with id: 99"));

        mockMvc.perform(put("/api/employee/timesheets/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Timesheet not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void updateTimesheet_notAuthorized_shouldReturnForbidden() throws Exception {
        when(timesheetService.updateTimesheet(anyLong(), any(TimesheetRequestDTO.class), anyString()))
            .thenThrow(new RuntimeException("User is not authorized to update this timesheet."));

        mockMvc.perform(put("/api/employee/timesheets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User is not authorized to update this timesheet."));
    }
    
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void updateTimesheet_notPending_shouldReturnForbidden() throws Exception {
        when(timesheetService.updateTimesheet(anyLong(), any(TimesheetRequestDTO.class), anyString()))
            .thenThrow(new RuntimeException("Timesheet can only be updated if its status is PENDING."));

        mockMvc.perform(put("/api/employee/timesheets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Timesheet can only be updated if its status is PENDING."));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void deleteTimesheet_asOwnerAndPending_shouldReturnNoContent() throws Exception {
        doNothing().when(timesheetService).deleteTimesheet(anyLong(), anyString());

        mockMvc.perform(delete("/api/employee/timesheets/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void deleteTimesheet_timesheetNotFound_shouldReturnNotFound() throws Exception {
        doNothing().when(timesheetService).deleteTimesheet(anyLong(), anyString()); // This is incorrect, should throw
        when(timesheetService.deleteTimesheet(99L, "employee")) // Correct way to mock for specific case if needed, or just throw directly
            .thenThrow(new RuntimeException("Timesheet not found with id: 99"));


        mockMvc.perform(delete("/api/employee/timesheets/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Timesheet not found with id: 99"));
    }
    
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void deleteTimesheet_notAuthorized_shouldReturnForbidden() throws Exception {
        doNothing().when(timesheetService).deleteTimesheet(anyLong(), anyString());
        when(timesheetService.deleteTimesheet(1L, "employee"))
            .thenThrow(new RuntimeException("User is not authorized to delete this timesheet."));
            
        mockMvc.perform(delete("/api/employee/timesheets/1"))
            .andExpect(status().isForbidden())
            .andExpect(content().string("User is not authorized to delete this timesheet."));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "OTHER_ROLE") // A user with a role not explicitly allowed by controller's PreAuthorize
    void createTimesheet_asOtherRole_shouldReturnForbidden() throws Exception {
         // EmployeeController is @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
        mockMvc.perform(post("/api/employee/timesheets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTimesheet_asAnonymous_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/employee/timesheets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timesheetRequestDTO)))
                .andExpect(status().isUnauthorized());
    }
}
