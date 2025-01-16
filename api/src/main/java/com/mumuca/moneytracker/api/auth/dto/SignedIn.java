package com.mumuca.moneytracker.api.auth.dto;

public record SignedIn(String accessToken, Long expiresIn) {}
