package com.example.wanteat.service;

import com.example.wanteat.domain.User;
import com.example.wanteat.dto.LoginRequest;
import com.example.wanteat.dto.LoginResponse;
import com.example.wanteat.dto.SignUpRequest;
import com.example.wanteat.dto.SignUpResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("パスワードが正しくありません");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginResponse(token);
    }

    @Transactional 
    public SignUpResponse signUp(SignUpRequest request){
        var user = userRepository.findByEmail(request.email());
        if(user.isPresent()){
            throw new IllegalArgumentException("ユーザーは既に存在します");
        }

        var createUser = new User();
        createUser.setEmail(request.email());
        createUser.setPassword(passwordEncoder.encode(request.password()));
        createUser.setRole("USER");

        var result = userRepository.save(createUser);

        String token = jwtService.generateToken(result.getId(), result.getEmail(), result.getRole());

        return new SignUpResponse(token);
    }
}
