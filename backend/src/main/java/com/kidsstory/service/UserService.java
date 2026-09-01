package com.kidsstory.service;

import com.kidsstory.dto.UserCreateRequest;
import com.kidsstory.entity.User;
import com.kidsstory.exception.ApiException;
import com.kidsstory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Ported from services/user_service.py; only exercised by the (unmounted) auth flow. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    public User createUser(UserCreateRequest payload) {
        userRepository.findByEmail(payload.email()).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        });

        User user = new User();
        user.setEmail(payload.email());
        user.setHashedPassword(securityUtil.hashPassword(payload.password()));
        user.setDisplayName(payload.displayName());
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !securityUtil.verifyPassword(password, user.getHashedPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }
}
