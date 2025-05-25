package com.example.timesheetappbackend.controller;

import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllUsers_asSuperAdmin_shouldReturnUserList() throws Exception {
        UserDTO user1 = new UserDTO(1L, "user1", "user1@example.com", UserRole.ROLE_EMPLOYEE, true, null, null);
        UserDTO user2 = new UserDTO(2L, "user2", "user2@example.com", UserRole.ROLE_MANAGER, true, null, null);
        List<UserDTO> users = Arrays.asList(user1, user2);

        when(userService.findAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE") // Non-admin user
    void getAllUsers_asEmployee_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void getAllUsers_asAnonymous_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized()); // or .isForbidden() if entry point handles it that way
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignManager_asSuperAdmin_shouldReturnUpdatedUser() throws Exception {
        UserDTO updatedUser = new UserDTO(1L, "employee", "emp@example.com", UserRole.ROLE_EMPLOYEE, true, 2L, "manager");
        when(userService.assignManager(anyLong(), anyLong())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/admin/users/1/assign-manager/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee"))
                .andExpect(jsonPath("$.managerId").value(2L));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignManager_userNotFound_shouldReturnBadRequest() throws Exception {
        when(userService.assignManager(anyLong(), anyLong())).thenThrow(new RuntimeException("User not found with id: 1"));

        mockMvc.perform(put("/api/admin/users/1/assign-manager/2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found with id: 1"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void assignManager_asEmployee_shouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/assign-manager/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void enableUser_asSuperAdmin_shouldReturnUpdatedUser() throws Exception {
        UserDTO enabledUser = new UserDTO(1L, "user1", "user1@example.com", UserRole.ROLE_EMPLOYEE, true, null, null);
        when(userService.enableUser(1L, true)).thenReturn(enabledUser);

        mockMvc.perform(put("/api/admin/users/1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.enabled").value(true));
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void enableUser_userNotFound_shouldReturnNotFound() throws Exception {
        when(userService.enableUser(99L, true)).thenThrow(new RuntimeException("User not found with id: 99"));
        mockMvc.perform(put("/api/admin/users/99/enable"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void enableUser_asEmployee_shouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/enable"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void disableUser_asSuperAdmin_shouldReturnUpdatedUser() throws Exception {
        UserDTO disabledUser = new UserDTO(1L, "user1", "user1@example.com", UserRole.ROLE_EMPLOYEE, false, null, null);
        when(userService.enableUser(1L, false)).thenReturn(disabledUser);

        mockMvc.perform(put("/api/admin/users/1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.enabled").value(false));
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void disableUser_userNotFound_shouldReturnNotFound() throws Exception {
        when(userService.enableUser(99L, false)).thenThrow(new RuntimeException("User not found with id: 99"));
        mockMvc.perform(put("/api/admin/users/99/disable"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void disableUser_asEmployee_shouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/disable"))
                .andExpect(status().isForbidden());
    }
}
