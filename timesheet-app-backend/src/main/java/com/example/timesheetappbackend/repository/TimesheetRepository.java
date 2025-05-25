package com.example.timesheetappbackend.repository;

import com.example.timesheetappbackend.model.Timesheet;
import com.example.timesheetappbackend.model.TimesheetStatus;
import com.example.timesheetappbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByEmployee(User employee);

    List<Timesheet> findByEmployeeAndStatus(User employee, TimesheetStatus status);

    List<Timesheet> findByEmployee_ManagerAndStatus(User manager, TimesheetStatus status);
}
