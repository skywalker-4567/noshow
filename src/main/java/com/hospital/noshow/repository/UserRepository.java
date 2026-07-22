package com.hospital.noshow.repository;

import com.hospital.noshow.entity.User;
import com.hospital.noshow.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // §11: used to email all Receptionists / find the Admin recipient
    List<User> findByRole(UserRole role);
}