package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 메소드 이름 규칙에 따라 자동으로 쿼리 생성:
    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);
}