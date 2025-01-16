package com.mumuca.moneytracker.api.auth.service;

import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;


public interface AuthService {
    void signUp(SignUpDTO signUpDTO);
}
