package com.example.timesheetappbackend.service;

import com.example.timesheetappbackend.dto.UserDTO;
import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO assignManager(Long userId, Long managerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found with id: " + managerId));

        if (manager.getRole() != UserRole.ROLE_MANAGER && manager.getRole() != UserRole.ROLE_SUPER_ADMIN) {
            throw new RuntimeException("User with id: " + managerId + " does not have the MANAGER or SUPER_ADMIN role.");
        }
        
        if (user.getId().equals(manager.getId())) {
            throw new RuntimeException("User cannot be their own manager.");
        }

        user.setManager(manager);
        User updatedUser = userRepository.save(user);
        return convertToUserDTO(updatedUser);
    }

    @Transactional
    public UserDTO enableUser(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(enable);
        User updatedUser = userRepository.save(user);
        return convertToUserDTO(updatedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getManagedEmployees(String managerUsername) {
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager not found with username: " + managerUsername));

        if (manager.getRole() != UserRole.ROLE_MANAGER && manager.getRole() != UserRole.ROLE_SUPER_ADMIN) {
            throw new RuntimeException("User " + managerUsername + " is not a manager.");
        }

        return manager.getManagedEmployees().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(user.getRole());
        userDTO.setEnabled(user.isEnabled());
        if (user.getManager() != null) {
            userDTO.setManagerId(user.getManager().getId());
            userDTO.setManagerName(user.getManager().getUsername());
        }
        return userDTO;
    }
}
