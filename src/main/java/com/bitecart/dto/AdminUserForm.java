package com.bitecart.dto;

import com.bitecart.entity.Role;
import jakarta.validation.constraints.NotNull;

public class AdminUserForm {
    @NotNull private Role role;
    private boolean enabled;
    public Role getRole() { return role; } public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
