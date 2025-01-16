package com.mumuca.moneytracker.api.auth.service.impl;

import com.mumuca.moneytracker.api.auth.dto.SignInDTO;
import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import com.mumuca.moneytracker.api.auth.dto.SignedIn;
import com.mumuca.moneytracker.api.auth.exception.CredentialsMismatchException;
import com.mumuca.moneytracker.api.auth.exception.UserAlreadyExistsException;
import com.mumuca.moneytracker.api.auth.model.Role;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.RoleRepository;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.auth.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Override
    public void signUp(SignUpDTO signUpDTO) {
        boolean userAlreadyExists = userRepository.existsByEmail(signUpDTO.email());

        if (userAlreadyExists) {
            throw new UserAlreadyExistsException(signUpDTO.email());
        }

        Role userRole = roleRepository
                .findByAuthority("USER")
                .orElseThrow();

        User user = new User();

        user.setRoles(Set.of(userRole));
        user.setFirstName(signUpDTO.firstName());
        user.setLastName(signUpDTO.lastName());
        user.setEmail(signUpDTO.email());
        user.setPassword(passwordEncoder.encode(signUpDTO.password()));

        userRepository.save(user);
    }

    @Override
    public SignedIn signIn(SignInDTO signInDTO) {
        var user = userRepository.findByEmail(signInDTO.email());

        if (user.isEmpty() || !passwordEncoder.matches(signInDTO.password(), user.get().getPassword())) {
            throw new CredentialsMismatchException();
        }

        var now = Instant.now();
        long expiresIn = 604800;

        List<String> roles = user.get().getRoles()
                .stream()
                .map(Role::getAuthority)
                .toList();

        var claims = JwtClaimsSet.builder()
                .issuer("Money Tracker")
                .subject(user.get().getId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("roles", roles)
                .build();

        var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new SignedIn(jwt, expiresIn);
    }
}
