package com.example.timesheetappbackend.service;

import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1, user2, manager, employee;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1", "pass", "user1@example.com", UserRole.ROLE_EMPLOYEE, true, null, new HashSet<>());
        user2 = new User(2L, "user2", "pass", "user2@example.com", UserRole.ROLE_EMPLOYEE, true, null, new HashSet<>());
        manager = new User(3L, "manager", "pass", "manager@example.com", UserRole.ROLE_MANAGER, true, null, new HashSet<>());
        employee = new User(4L, "employee", "pass", "employee@example.com", UserRole.ROLE_EMPLOYEE, true, manager, new HashSet<>());
        manager.getManagedEmployees().add(employee); // Make employee managed by manager
    }

    @Test
    void findAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<UserDTO> userDTOs = userService.findAllUsers();

        assertEquals(2, userDTOs.size());
        assertEquals(user1.getUsername(), userDTOs.get(0).getUsername());
        assertEquals(user2.getUsername(), userDTOs.get(1).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void assignManager_successful() {
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userRepository.save(any(User.class))).thenReturn(employee); // employee will be updated

        UserDTO userDTO = userService.assignManager(employee.getId(), manager.getId());

        assertNotNull(userDTO);
        assertEquals(manager.getId(), userDTO.getManagerId());
        assertEquals(manager.getUsername(), userDTO.getManagerName());
        verify(userRepository).findById(employee.getId());
        verify(userRepository).findById(manager.getId());
        verify(userRepository).save(employee);
    }

    @Test
    void assignManager_userNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.assignManager(99L, manager.getId()));
        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(99L);
        verify(userRepository, never()).findById(manager.getId());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void assignManager_managerNotFound() {
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.assignManager(employee.getId(), 99L));
        assertEquals("Manager not found with id: 99", exception.getMessage());
        verify(userRepository).findById(employee.getId());
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignManager_targetManagerNotManagerRole() {
        User notAManager = new User(5L, "notmanager", "pass", "notmanager@example.com", UserRole.ROLE_EMPLOYEE, true, null, new HashSet<>());
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(userRepository.findById(notAManager.getId())).thenReturn(Optional.of(notAManager));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.assignManager(employee.getId(), notAManager.getId()));
        assertEquals("User with id: 5 does not have the MANAGER or SUPER_ADMIN role.", exception.getMessage());
        verify(userRepository).findById(employee.getId());
        verify(userRepository).findById(notAManager.getId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignManager_selfAssignment() {
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        // We need to findById for manager.getId() twice, once for user, once for manager
        // when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager)); 

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.assignManager(manager.getId(), manager.getId()));
        assertEquals("User cannot be their own manager.", exception.getMessage());
        verify(userRepository, times(2)).findById(manager.getId()); // Called for user and manager
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void enableUser_enable() {
        User disabledUser = new User(6L, "disabled", "pass", "disabled@example.com", UserRole.ROLE_EMPLOYEE, false, null, new HashSet<>());
        when(userRepository.findById(disabledUser.getId())).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO userDTO = userService.enableUser(disabledUser.getId(), true);

        assertTrue(userDTO.isEnabled());
        verify(userRepository).findById(disabledUser.getId());
        verify(userRepository).save(disabledUser);
    }

    @Test
    void enableUser_disable() {
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO userDTO = userService.enableUser(user1.getId(), false);

        assertFalse(userDTO.isEnabled());
        verify(userRepository).findById(user1.getId());
        verify(userRepository).save(user1);
    }
    
    @Test
    void enableUser_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.enableUser(99L, true));
        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getManagedEmployees_managerFoundWithEmployees() {
        when(userRepository.findByUsername(manager.getUsername())).thenReturn(Optional.of(manager));
        // manager already has 'employee' in managedEmployees set up in @BeforeEach

        List<UserDTO> managedEmployeesDTO = userService.getManagedEmployees(manager.getUsername());

        assertEquals(1, managedEmployeesDTO.size());
        assertEquals(employee.getUsername(), managedEmployeesDTO.get(0).getUsername());
        verify(userRepository).findByUsername(manager.getUsername());
    }

    @Test
    void getManagedEmployees_managerFoundWithNoEmployees() {
        User managerWithNoEmployees = new User(5L, "lonelymanager", "pass", "lonely@example.com", UserRole.ROLE_MANAGER, true, null, new HashSet<>());
        when(userRepository.findByUsername(managerWithNoEmployees.getUsername())).thenReturn(Optional.of(managerWithNoEmployees));

        List<UserDTO> managedEmployeesDTO = userService.getManagedEmployees(managerWithNoEmployees.getUsername());

        assertTrue(managedEmployeesDTO.isEmpty());
        verify(userRepository).findByUsername(managerWithNoEmployees.getUsername());
    }

    @Test
    void getManagedEmployees_userIsNotAManager() {
        when(userRepository.findByUsername(employee.getUsername())).thenReturn(Optional.of(employee)); // 'employee' is not a manager

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.getManagedEmployees(employee.getUsername()));
        assertEquals("User " + employee.getUsername() + " is not a manager.", exception.getMessage());
        verify(userRepository).findByUsername(employee.getUsername());
    }
    
    @Test
    void getManagedEmployees_managerNotFound() {
        when(userRepository.findByUsername("unknownManager")).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.getManagedEmployees("unknownManager"));
        assertEquals("Manager not found with username: unknownManager", exception.getMessage());
        verify(userRepository).findByUsername("unknownManager");
    }
}
