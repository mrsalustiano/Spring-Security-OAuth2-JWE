package com.example.oauth2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Role {
    private Long id;
    private String roleName;
    private String descricao;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}