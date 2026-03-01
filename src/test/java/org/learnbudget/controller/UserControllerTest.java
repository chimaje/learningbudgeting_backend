package org.learnbudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.learnbudget.dto.request.UpdateUserRequest;
import org.learnbudget.dto.response.UserResponse;
import org.learnbudget.model.User;
import org.learnbudget.exception.UnauthorizedException;
import org.learnbudget.exception.UserNotFoundException;
import org.learnbudget.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Controller Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        testUser = userRepository.save(testUser);

        updateUserRequest = UpdateUserRequest.builder()
                .firstName("Johnny")
                .lastName("Updated")
                .build();
    }

    // ==================== GET USER BY ID TESTS ====================

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @WithMockUser
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserById() throws Exception {
            mockMvc.perform(get("/api/users/" + testUser.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                    .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            mockMvc.perform(get("/api/users/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== GET USER BY EMAIL TESTS ====================

    @Nested
    @DisplayName("Get User By Email Tests")
    class GetUserByEmailTests {

        @Test
        @WithMockUser
        @DisplayName("Should get user by email successfully")
        void shouldGetUserByEmail() throws Exception {
            mockMvc.perform(get("/api/users/email/john.doe@example.com"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.email", is("john.doe@example.com")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when email not found")
        void shouldReturn404WhenEmailNotFound() throws Exception {
            mockMvc.perform(get("/api/users/email/notfound@example.com"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== GET ALL USERS TESTS ====================

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @WithMockUser
        @DisplayName("Should get all users successfully")
        void shouldGetAllUsers() throws Exception {
            User user2 = User.builder()
                    .email("jane@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();
            userRepository.save(user2);

            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsers() throws Exception {
            userRepository.deleteAll();

            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET CURRENT USER TESTS ====================

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @WithMockUser(username = "john.doe@example.com")
        @DisplayName("Should get current user successfully")
        void shouldGetCurrentUser() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.email", is("john.doe@example.com")));
        }
    }

    // ==================== UPDATE USER TESTS ====================

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @WithMockUser(username = "john.doe@example.com")
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            mockMvc.perform(put("/api/users/" + testUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.firstName", is("Johnny")))
                    .andExpect(jsonPath("$.lastName", is("Updated")));
        }

        @Test
        @WithMockUser(username = "other@example.com")
        @DisplayName("Should return 403 when updating another user's profile")
        void shouldReturn403WhenUpdatingAnotherUser() throws Exception {
            mockMvc.perform(put("/api/users/" + testUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            mockMvc.perform(put("/api/users/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== DELETE USER TESTS ====================

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @WithMockUser(username = "john.doe@example.com")
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() throws Exception {
            mockMvc.perform(delete("/api/users/" + testUser.getId()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "other@example.com")
        @DisplayName("Should return 403 when deleting another user")
        void shouldReturn403WhenDeletingAnotherUser() throws Exception {
            mockMvc.perform(delete("/api/users/" + testUser.getId()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            mockMvc.perform(delete("/api/users/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}