package com.pastline.auth.dto;

import com.pastline.auth.entity.User;
import java.util.UUID;

/** Сутність User назовні не віддається: лише ці поля. */
public record UserResponse(UUID id, String email, String timezone) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getTimezone());
    }
}
