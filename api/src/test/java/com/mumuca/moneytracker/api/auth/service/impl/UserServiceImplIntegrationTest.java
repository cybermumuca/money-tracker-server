package com.mumuca.moneytracker.api.auth.service.impl;

import com.mumuca.moneytracker.api.auth.dto.UserDTO;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.createUser;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserServiceImpl Integration Tests")
class UserServiceImplIntegrationTest {

    @Autowired
    private UserServiceImpl sut;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("getUser tests")
    class GetUserTests {
        @Test
        @Transactional
        @DisplayName("should be able to get user")
        void shouldBeAbleToGetUser() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act
            UserDTO result = sut.getUser(user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(user.getId());
            assertThat(result.email()).isEqualTo(user.getEmail());
            assertThat(result.firstName()).isEqualTo(user.getFirstName());
            assertThat(result.lastName()).isEqualTo(user.getLastName());
            assertThat(result.gender()).isEqualTo(user.getGender());
            assertThat(result.picture()).isEqualTo(user.getPhotoUrl());
            assertThat(result.age()).isEqualTo(user.getAge());
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowResourceNotFoundExceptionIfUserNotFound() {
            assertThatThrownBy(() -> sut.getUser(randomUUID().toString()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }
}