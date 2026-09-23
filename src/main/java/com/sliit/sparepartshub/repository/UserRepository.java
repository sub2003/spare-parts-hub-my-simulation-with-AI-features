package com.sliit.sparepartshub.repository;

import com.sliit.sparepartshub.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Integer userId);

    boolean existsByUserCodeIgnoreCase(String userCode);

    boolean existsByUserCodeIgnoreCaseAndUserIdNot(String userCode, Integer userId);

    long countByRoleAndActiveTrue(User.Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findByRoleAndActiveTrue(User.Role role);

    List<User> findAllByOrderByNameAsc();
}
