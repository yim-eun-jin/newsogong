package com.example.codegardener.post.repository;

import com.example.codegardener.post.domain.Post;
import com.example.codegardener.post.domain.PostScraps;
import com.example.codegardener.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScraps, Long> {
    long countByPost(Post post);
    List<PostScraps> findAllByUser(User user);
    Optional<PostScraps> findByUserAndPost(User user, Post post);
}