package com.mumuca.moneytracker.api.auth.service.impl;

import com.mumuca.moneytracker.api.auth.dto.SignInDTO;
import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import com.mumuca.moneytracker.api.auth.dto.SignedIn;
import com.mumuca.moneytracker.api.auth.exception.CredentialsMismatchException;
import com.mumuca.moneytracker.api.auth.exception.UserAlreadyExistsException;
import com.mumuca.moneytracker.api.auth.model.Gender;
import com.mumuca.moneytracker.api.auth.model.Role;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.RoleRepository;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AuthServiceImpl Integration Tests")
class AuthServiceImplIntegrationTest {
    @Autowired
    private AuthServiceImpl sut;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("signUp tests")
    class SignUpTests {
        @Test
        @Transactional
        @DisplayName("should be able to sign up")
        void shouldBeAbleToSignUp() {
            // Arrange
            String firstName = "Samuel";
            String lastName = "Laurindo";
            Gender gender = Gender.MALE;
            LocalDate birthday = LocalDate.of(2004, 1, 1);
            String email = "samuel@email.com";
            String password = "12345678";

            Role role = new Role();
            role.setAuthority("USER");
            roleRepository.save(role);

            SignUpDTO signUpDTO = new SignUpDTO(
                    firstName,
                    lastName,
                    gender,
                    birthday,
                    email,
                    password
            );

            // Act
            sut.signUp(signUpDTO);

            // Assert
            var userInDatabase = userRepository
                    .findByEmail(email)
                    .orElseThrow();

            assertThat(userInDatabase).isNotNull();
            assertThat(userInDatabase.getEmail()).isEqualTo(email);
            assertThat(passwordEncoder.matches(password, userInDatabase.getPassword())).isTrue();
            assertThat(userInDatabase.getFirstName()).isEqualTo(firstName);
            assertThat(userInDatabase.getLastName()).isEqualTo(lastName);
            assertThat(userInDatabase.getGender()).isEqualTo(gender);
            assertThat(userInDatabase.getBirthDate()).isEqualTo(birthday);
            assertThat(userInDatabase.getRoles())
                    .hasSize(1)
                    .extracting("authority")
                    .contains("USER");
        }

        @Test
        @Transactional
        @DisplayName("should throw UserAlreadyExistsException if email is already registered")
        void shouldThrowUserAlreadyExistsExceptionIfEmailIsAlreadyRegistered() {
            // Arrange
            String email = "samuel@email.com";

            User user = User.builder()
                    .firstName("Samuel")
                    .lastName("Laurindo")
                    .password(passwordEncoder.encode("12345678"))
                    .email(email)
                    .birthDate(LocalDate.of(2004, 1, 1))
                    .gender(Gender.MALE)
                    .photoUrl(null)
                    .build();

            userRepository.save(user);

            SignUpDTO signUpDTO = new SignUpDTO(
                    "firstName",
                    "lastName",
                    Gender.MALE,
                    LocalDate.now().minusDays(1),
                    email,
                    "password"
            );

            // Act & Assert
            assertThatThrownBy(() -> sut.signUp(signUpDTO))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("User already registered with email: " + email);
        }
    }

    @Nested
    @DisplayName("signIn tests")
    class SignInTests {
        @Test
        @Transactional
        @DisplayName("should sign in with valid credentials")
        void shouldSignInWithValidCredentials() {
            // Arrange
            String email = "samuel@email.com";
            String password = "12345678";
            String hashedPassword = passwordEncoder.encode(password);

            Role userRole = new Role();
            userRole.setAuthority("USER");
            roleRepository.save(userRole);

            User user = User.builder()
                    .firstName("Samuel")
                    .lastName("Laurindo")
                    .email(email)
                    .password(hashedPassword)
                    .roles(Set.of(userRole))
                    .build();

            userRepository.save(user);

            SignInDTO signInDTO = new SignInDTO(email, password);

            // Act
            SignedIn response = sut.signIn(signInDTO);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.expiresIn()).isEqualTo(604800);

            var tokenDecoded = jwtDecoder.decode(response.accessToken());

            assertThat(tokenDecoded).isNotNull();
            assertThat(tokenDecoded.getSubject()).isEqualTo(user.getId());
            assertThat(tokenDecoded.getClaimAsStringList("roles")).hasSize(1).contains("USER");
        }

        @Test
        @Transactional
        @DisplayName("should throw CredentialsMismatchException if email does not exist")
        void shouldThrowCredentialsMismatchExceptionIfEmailDoesNotExist() {
            // Arrange
            SignInDTO signInDTO = new SignInDTO("nonexistent@email.com", "12345678");

            // Act & Assert
            assertThatThrownBy(() -> sut.signIn(signInDTO))
                    .isInstanceOf(CredentialsMismatchException.class)
                    .hasMessageContaining("Email or password is invalid.");
        }

        @Test
        @Transactional
        @DisplayName("should throw CredentialsMismatchException if password is incorrect")
        void shouldThrowCredentialsMismatchExceptionIfPasswordIsIncorrect() {
            // Arrange
            String email = "samuel@email.com";
            String password = "12345678";
            String hashedPassword = passwordEncoder.encode(password);

            Role userRole = new Role();
            userRole.setAuthority("USER");
            roleRepository.save(userRole);

            User user = User.builder()
                    .firstName("Samuel")
                    .lastName("Laurindo")
                    .email(email)
                    .password(hashedPassword)
                    .roles(Set.of(userRole))
                    .build();

            userRepository.save(user);

            SignInDTO signInDTO = new SignInDTO(email, "wrongpassword");

            // Act & Assert
            assertThatThrownBy(() -> sut.signIn(signInDTO))
                    .isInstanceOf(CredentialsMismatchException.class)
                    .hasMessageContaining("Email or password is invalid.");
        }
    }

    @Nested
    @DisplayName("signOut tests")
    class SignOutTests {
        @Test
        @Disabled("jwtblacklist not implemented yet.")
        @DisplayName("should add token to blacklist on sign out")
        void shouldAddTokenToBlacklistOnSignOut() {
            // Arrange
            String token = "some-valid-token";
            Instant expiresAt = Instant.now().plusSeconds(3600);

            // Act
            sut.signOut(token, expiresAt);

            // Assert
            //assertThat(jwtBlacklist.isTokenBlacklisted(token)).isTrue();
        }
    }
}