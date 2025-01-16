package com.mumuca.moneytracker.api.auth.service;

import com.mumuca.moneytracker.api.auth.dto.SignInDTO;
import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import com.mumuca.moneytracker.api.auth.dto.SignedIn;


public interface AuthService {
    void signUp(SignUpDTO signUpDTO);
    SignedIn signIn(SignInDTO signInDTO);
}
