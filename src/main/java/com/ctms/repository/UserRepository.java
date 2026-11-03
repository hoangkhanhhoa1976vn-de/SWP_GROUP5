package com.ctms.repository;

import com.ctms.model.Role;
import com.ctms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    List<User> findByRole(Role role);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
}
