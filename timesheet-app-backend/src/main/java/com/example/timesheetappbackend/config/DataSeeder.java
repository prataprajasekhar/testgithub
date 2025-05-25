package com.example.timesheetappbackend.config;

import com.example.timesheetappbackend.model.User;
import com.example.timesheetappbackend.model.UserRole;
import com.example.timesheetappbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.existsByUsername("superadmin")) {
            logger.info("Default users already exist. Skipping data seeding.");
            return;
        }

        logger.info("Starting data seeding...");

        // Create Super Admin User
        User superAdmin = new User();
        superAdmin.setUsername("superadmin");
        superAdmin.setPassword(passwordEncoder.encode("password"));
        superAdmin.setEmail("superadmin@example.com");
        superAdmin.setRole(UserRole.ROLE_SUPER_ADMIN);
        superAdmin.setEnabled(true);
        userRepository.save(superAdmin);
        logger.info("Created Super Admin: superadmin");

        // Create Manager User
        User manager = new User();
        manager.setUsername("manager");
        manager.setPassword(passwordEncoder.encode("password"));
        manager.setEmail("manager@example.com");
        manager.setRole(UserRole.ROLE_MANAGER);
        manager.setEnabled(true);
        userRepository.save(manager);
        logger.info("Created Manager: manager");

        // Create Employee User
        User employee = new User();
        employee.setUsername("employee");
        employee.setPassword(passwordEncoder.encode("password"));
        employee.setEmail("employee@example.com");
        employee.setRole(UserRole.ROLE_EMPLOYEE);
        employee.setEnabled(true);
        employee.setManager(manager); // Assign the created manager
        userRepository.save(employee);
        logger.info("Created Employee: employee (managed by manager)");

        logger.info("Data seeding completed.");
    }
}
