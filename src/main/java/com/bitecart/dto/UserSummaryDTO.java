package com.bitecart.dto;

import com.bitecart.entity.Role;
import java.time.LocalDateTime;
public record UserSummaryDTO(Long id, String firstName, String lastName, String email, String phone, Role role, boolean enabled, LocalDateTime createdAt) { }
