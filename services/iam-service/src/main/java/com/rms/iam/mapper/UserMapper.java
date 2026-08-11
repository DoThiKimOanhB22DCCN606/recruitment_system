package com.rms.iam.mapper;

import com.rms.iam.dto.response.UserDto;
import com.rms.iam.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        if (user.getTenant() != null) {
            dto.setTenantId(user.getTenant().getId());
        }
        return dto;
    }
}
