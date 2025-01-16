package com.mumuca.moneytracker.api.auth.service.impl;

import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import com.mumuca.moneytracker.api.auth.exception.UserAlreadyExistsException;
import com.mumuca.moneytracker.api.auth.model.Role;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.RoleRepository;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.auth.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
}
