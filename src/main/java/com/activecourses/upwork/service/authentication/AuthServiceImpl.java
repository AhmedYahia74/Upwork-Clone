package com.activecourses.upwork.service.authentication;

import com.activecourses.upwork.config.security.CustomeUserDetailsService;
import com.activecourses.upwork.dto.ResponseDto;
import com.activecourses.upwork.dto.authentication.login.LoginRequestDto;
import com.activecourses.upwork.dto.authentication.registration.RegistrationRequestDto;
import com.activecourses.upwork.dto.authentication.registration.RegistrationResponseDto;
import com.activecourses.upwork.mapper.Mapper;
import com.activecourses.upwork.model.RefreshToken;
import com.activecourses.upwork.model.User;
import com.activecourses.upwork.repository.UserRepository;
import com.activecourses.upwork.config.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final Mapper<User, RegistrationRequestDto> userMapper;
    private final CustomeUserDetailsService customeUserDetailsService;
    private final RefreshTokenService refreshTokenService;


    @Override
    public RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto) {
        User user = userMapper.mapFrom(registrationRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return RegistrationResponseDto
                .builder()
                .message("User registered successfully, please verify your email")
                .build();
    }

    @Transactional
    @Override
    public ResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = customeUserDetailsService.loadUserByUsername(loginRequestDto.getEmail());

        ResponseCookie jwtCookie = jwtService.generateJwtCookie(userDetails);

        int userId = ((User) userDetails).getId();

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId);

        ResponseCookie refreshJwtCookie = jwtService.generateRefreshJwtCookie(refreshToken.getToken());

        return ResponseDto
                .builder()
                .status(HttpStatus.OK)
                .success(true)
                .data(Map.of("jwtCookie", jwtCookie, "refreshJwtCookie", refreshJwtCookie))
                .build();
    }

    @Override
    public ResponseEntity<ResponseDto> logout() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!principal.toString().equals("anonymousUser")) {
            int userId = ((User) principal).getId();
            refreshTokenService.deleteByUserId(userId);
        }

        ResponseCookie jwtCookie = jwtService.getCleanJwtCookie();
        ResponseCookie refreshJwtCookie = jwtService.getCleanJwtRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshJwtCookie.toString())
                .body(ResponseDto
                        .builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data("User logged out successfully!")
                        .build()
                );
    }

    @Override
    public Optional<User> refreshToken(String refreshToken) {
        String username = jwtService.getUserNameFromJwtToken(refreshToken);
        return userRepository.findByEmail(username);
    }

    @Override
    public boolean verifyUser(String token) {
        Optional<User> user = userRepository.findByVerificationToken(token);
        User unwrappedUser = unwrapUser(user);
        if (unwrappedUser != null) {
            unwrappedUser.setAccountEnabled(true);
            unwrappedUser.setVerificationToken(null); // Clear the token
            userRepository.save(unwrappedUser);
            return true;
        }
        return false;
    }

    @Override
    public User findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return unwrapUser(user);
    }

    @Override
    public void sendVerificationEmail(User user) {
        String verificationLink = "http://localhost:8080/api/users/verify?token=" + user.getVerificationToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Email Verification");
        message.setText("Click the link to verify your email: " + verificationLink);
        mailSender.send(message);
    }

    static User unwrapUser(Optional<User> entity) {
        if (entity.isPresent()) return entity.get();
        else throw new UnsupportedOperationException("Unimplemented method 'unwrapUser'");
    }
}