package com.rms.iam.dto.response;

import com.rms.iam.entity.Role;
import com.rms.iam.entity.UserStatus;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String email;
    private Role role;
    private UserStatus status;
    private UUID tenantId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
