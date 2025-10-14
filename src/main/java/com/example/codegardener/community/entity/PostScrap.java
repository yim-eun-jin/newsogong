package com.example.codegardener.community.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Post_Scraps")
@IdClass(PostScrapId.class)
@Getter
@Setter
@NoArgsConstructor
public class PostScrap {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "post_id")
    private Long postId;
}