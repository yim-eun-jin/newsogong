package com.example.codegardener.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.codegardener.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);

    List<User> findTop3ByOrderByUserProfile_PointsDesc();
    Page<User> findAllByOrderByUserProfile_PointsDesc(Pageable pageable);
}