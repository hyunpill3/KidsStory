package com.kidsstory.controller;

import com.kidsstory.dto.TokenResponse;
import com.kidsstory.dto.UserCreateRequest;
import com.kidsstory.dto.UserLoginRequest;
import com.kidsstory.dto.UserResponse;
import com.kidsstory.entity.User;
import com.kidsstory.service.SecurityUtil;
import com.kidsstory.service.UserService;

/**
 * Ported from app/api/v1/auth.py, but - like the Python original -
 * deliberately NOT a Spring bean (no @RestController/@RequestMapping), so
 * it is never registered with the dispatcher and no /auth/* endpoint
 * exists. The MVP is anonymous/no-signup (see AnonIdentityArgumentResolver);
 * this is kept for a future signed-in flow.
 */
public class AuthController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    public AuthController(UserService userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    public UserResponse register(UserCreateRequest payload) {
        User user = userService.createUser(payload);
        return UserResponse.from(user);
    }

    public TokenResponse login(UserLoginRequest payload) {
        User user = userService.authenticate(payload.email(), payload.password());
        return new TokenResponse(securityUtil.createAccessToken(user.getId().toString()));
    }
}
