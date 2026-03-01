package org.learnbudget.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.learnbudget.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Repository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .email("john.doe@example.com")
                .password("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .build();

        anotherUser = User.builder()
                .email("jane.smith@example.com")
                .password("hashedPassword456")
                .firstName("Jane")
                .lastName("Smith")
                .build();
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    @Nested //groups related test together
    @DisplayName("CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save user successfully")
        void shouldSaveUser() {
            // Act
            User savedUser = userRepository.save(testUser);

            // Assert
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(savedUser.getFirstName()).isEqualTo("John");
            assertThat(savedUser.getLastName()).isEqualTo("Doe");
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should generate ID automatically when saving user")
        void shouldGenerateIdAutomatically() {
            // Act
            User savedUser = userRepository.save(testUser);

            // Assert
            assertThat(savedUser.getId()).isGreaterThan(0L);
        }

        @Test
        @DisplayName("Should save multiple users")
        void shouldSaveMultipleUsers() {
            // Act
            User saved1 = userRepository.save(testUser);
            User saved2 = userRepository.save(anotherUser);

            // Assert
            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            // Arrange
            User savedUser = userRepository.save(testUser);

            // Act
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should return empty optional when user ID not found")
        void shouldReturnEmptyWhenIdNotFound() {
            // Act
            Optional<User> foundUser = userRepository.findById(999L);

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUser() {
            // Arrange
            User savedUser = userRepository.save(testUser);

            // Act - update user
            savedUser.setFirstName("Johnny");
            savedUser.setLastName("Updated");
            User updatedUser = userRepository.save(savedUser);

            // Assert
            assertThat(updatedUser.getId()).isEqualTo(savedUser.getId());
            assertThat(updatedUser.getFirstName()).isEqualTo("Johnny");
            assertThat(updatedUser.getLastName()).isEqualTo("Updated");
            assertThat(updatedUser.getUpdatedAt()).isAfter(savedUser.getCreatedAt());
        }
        @Test
        @DisplayName("Should update last name by email")
        void shouldUpdateLastNameByEmail() {
            // Arrange
            userRepository.save(testUser);

            // Act
            userRepository.updateLastNameByEmail("john.doe@example.com", "LastName");
            userRepository.flush();

            entityManager.clear();

            // Assert
            Optional<User> updatedUser = userRepository.findByEmail("john.doe@example.com");
            assertThat(updatedUser).isPresent();
            assertThat(updatedUser.get().getLastName()).isEqualTo("LastName");
        }
        @Test
        @DisplayName("Should delete user by ID")
        void shouldDeleteUserById() {
            // Arrange
            User savedUser = userRepository.save(testUser);
            Long userId = savedUser.getId();

            // Act
            userRepository.deleteById(userId);

            // Assert
            assertThat(userRepository.findById(userId)).isEmpty();
            assertThat(userRepository.count()).isEqualTo(0);
        }
        @Test
        @DisplayName("Should delete user by email")
        void shouldDeleteUserByEmail() {
            // Arrange
            userRepository.save(testUser);

            // Act
            userRepository.deleteByEmail("john.doe@example.com");
            userRepository.flush();

            // Assert
            assertThat(userRepository.existsByEmail("john.doe@example.com")).isFalse();
            assertThat(userRepository.count()).isEqualTo(0);
        }
        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            // Arrange
            userRepository.save(testUser);
            userRepository.save(anotherUser);

            // Act
            List<User> allUsers = userRepository.findAll();

            // Assert
            assertThat(allUsers).hasSize(2);
            assertThat(allUsers)
                    .extracting(User::getEmail)
                    .containsExactlyInAnyOrder("john.doe@example.com", "jane.smith@example.com");
        }

        @Test
        @DisplayName("Should count users correctly")
        void shouldCountUsers() {
            // Arrange
            userRepository.save(testUser);
            userRepository.save(anotherUser);

            // Act
            long count = userRepository.count();

            // Assert
            assertThat(count).isEqualTo(2);
        }
    }

    // ==================== EMAIL-BASED QUERIES ====================

    @Nested
    @DisplayName("Email-Based Query Operations")
    class EmailBasedQueries {

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // Arrange
            userRepository.save(testUser);

            // Act
            Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getFirstName()).isEqualTo("John");
            assertThat(foundUser.get().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            // Act
            Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

            // Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should check if email exists")
        void shouldCheckEmailExists() {
            // Arrange
            userRepository.save(testUser);

            // Act
            boolean exists = userRepository.existsByEmail("john.doe@example.com");
            boolean notExists = userRepository.existsByEmail("notfound@example.com");

            // Assert
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }

        @Test
        @DisplayName("Should not throw error when deleting non-existent email")
        void shouldNotThrowErrorWhenDeletingNonExistentEmail() {
            // Act & Assert - should not throw exception
            userRepository.deleteByEmail("nonexistent@example.com");
            userRepository.flush();

            assertThat(userRepository.count()).isEqualTo(0);
        }
    }

    // ==================== CONSTRAINT TESTS ====================

    @Nested
    @DisplayName("Database Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("Should not allow duplicate emails")
        void shouldNotAllowDuplicateEmails() {
            // Arrange
            userRepository.save(testUser);

            User duplicateEmailUser = User.builder()
                    .email("john.doe@example.com") // Same email
                    .password("differentPassword")
                    .firstName("Different")
                    .lastName("Person")
                    .build();

            // Act & Assert
            assertThrows(DataIntegrityViolationException.class, () -> {
                userRepository.save(duplicateEmailUser);
                userRepository.flush();
            });
        }

        @Test
        @DisplayName("Should allow same name but different email")
        void shouldAllowSameNameDifferentEmail() {
            // Arrange
            User user1 = User.builder()
                    .email("john1@example.com")
                    .password("password1")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            User user2 = User.builder()
                    .email("john2@example.com")
                    .password("password2")
                    .firstName("John") // Same name
                    .lastName("Doe")   // Same name
                    .build();

            // Act
            userRepository.save(user1);
            userRepository.save(user2);

            // Assert
            assertThat(userRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Email should be case-sensitive for storage")
        void emailShouldBeCaseSensitive() {
            // Arrange
            User user1 = User.builder()
                    .email("John@Example.com")
                    .password("password1")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            User user2 = User.builder()
                    .email("john@example.com")
                    .password("password2")
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();

            // Act
            userRepository.save(user1);
            userRepository.save(user2);

            // Assert
            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(userRepository.findByEmail("John@Example.com")).isPresent();
            assertThat(userRepository.findByEmail("john@example.com")).isPresent();
        }
    }

    // ==================== NAME-BASED SEARCHES ====================

    @Nested
    @DisplayName("Name-Based Search Operations")
    class NameBasedSearches {

        @Test
        @DisplayName("Should find users by first name ignoring case")
        void shouldFindByFirstNameIgnoreCase() {
            // Arrange
            userRepository.save(testUser); // John
            userRepository.save(anotherUser); // Jane

            User anotherJohn = User.builder()
                    .email("john2@example.com")
                    .password("pass")
                    .firstName("john") // lowercase
                    .lastName("Another")
                    .build();
            userRepository.save(anotherJohn);

            // Act
            List<User> johnsUpperCase = userRepository.findByFirstNameIgnoreCase("JOHN");
            List<User> johnsLowerCase = userRepository.findByFirstNameIgnoreCase("john");
            List<User> janes = userRepository.findByFirstNameIgnoreCase("Jane");

            // Assert
            assertThat(johnsUpperCase).hasSize(2);
            assertThat(johnsLowerCase).hasSize(2);
            assertThat(janes).hasSize(1);
        }

        @Test
        @DisplayName("Should find users by last name ignoring case")
        void shouldFindByLastNameIgnoreCase() {
            // Arrange
            userRepository.save(testUser); // Doe
            userRepository.save(anotherUser); // Smith

            // Act
            List<User> does = userRepository.findByLastNameIgnoreCase("doe");
            List<User> smiths = userRepository.findByLastNameIgnoreCase("SMITH");

            // Assert
            assertThat(does).hasSize(1);
            assertThat(does.get(0).getFirstName()).isEqualTo("John");
            assertThat(smiths).hasSize(1);
            assertThat(smiths.get(0).getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("Should return empty list when name not found")
        void shouldReturnEmptyListWhenNameNotFound() {
            // Arrange
            userRepository.save(testUser);

            // Act
            List<User> result = userRepository.findByFirstNameIgnoreCase("NonExistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ==================== DATE-BASED QUERIES ====================

    @Nested
    @DisplayName("Date-Based Query Operations")
    class DateBasedQueries {

        @Test
        @DisplayName("Should find users created after a specific date")
        void shouldFindUsersCreatedAfter() throws InterruptedException {
            // Arrange
            userRepository.save(testUser);
            LocalDateTime cutoffDate = LocalDateTime.now();

            Thread.sleep(100); // Small delay

            userRepository.save(anotherUser);

            // Act
            List<User> usersAfterCutoff = userRepository.findByCreatedAtAfter(cutoffDate);

            // Assert
            assertThat(usersAfterCutoff).hasSize(1);
            assertThat(usersAfterCutoff.get(0).getEmail()).isEqualTo("jane.smith@example.com");
        }

        @Test
        @DisplayName("Should find users created between dates")
        void shouldFindUsersCreatedBetween() {
            // Arrange
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            userRepository.save(testUser);
            userRepository.save(anotherUser);

            // Act
            List<User> usersBetween = userRepository.findByCreatedAtBetween(startDate, endDate);

            // Assert
            assertThat(usersBetween).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty when no users in date range")
        void shouldReturnEmptyWhenNoUsersInDateRange() {
            // Arrange
            userRepository.save(testUser);

            LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);

            // Act
            List<User> users = userRepository.findByCreatedAtBetween(futureStart, futureEnd);

            // Assert
            assertThat(users).isEmpty();
        }
    }

    // ==================== CUSTOM QUERIES ====================

    @Nested
    @DisplayName("Custom Query Operations")
    class CustomQueries {

        @Test
        @DisplayName("Should count total users correctly")
        void shouldCountTotalUsers() {
            // Arrange
            userRepository.save(testUser);
            userRepository.save(anotherUser);

            // Act
            long count = userRepository.countTotalUsers();

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero when no users exist")
        void shouldReturnZeroWhenNoUsers() {
            // Act
            long count = userRepository.countTotalUsers();

            // Assert
            assertThat(count).isEqualTo(0);
        }


        @Test
        @DisplayName("Should not throw error when updating non-existent email")
        void shouldNotThrowErrorWhenUpdatingNonExistentEmail() {
            // Act & Assert - should not throw exception
            userRepository.updateLastNameByEmail("nonexistent@example.com", "NewName");
            userRepository.flush();
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty database queries")
        void shouldHandleEmptyDatabase() {
            // Act
            List<User> allUsers = userRepository.findAll();
            long count = userRepository.count();
            Optional<User> user = userRepository.findByEmail("any@example.com");

            // Assert
            assertThat(allUsers).isEmpty();
            assertThat(count).isEqualTo(0);
            assertThat(user).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long names")
        void shouldHandleVeryLongNames() {
            // Arrange
            String longName = "A".repeat(100); // 100 character name
            User userWithLongName = User.builder()
                    .email("long@example.com")
                    .password("password")
                    .firstName(longName)
                    .lastName(longName)
                    .build();

            // Act
            User saved = userRepository.save(userWithLongName);

            // Assert
            assertThat(saved.getFirstName()).hasSize(100);
            assertThat(saved.getLastName()).hasSize(100);
        }

        @Test
        @DisplayName("Should preserve email format exactly")
        void shouldPreserveEmailFormat() {
            // Arrange
            String complexEmail = "user+tag@sub.domain.example.com";
            User user = User.builder()
                    .email(complexEmail)
                    .password("password")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            // Act
            User saved = userRepository.save(user);

            // Assert
            assertThat(saved.getEmail()).isEqualTo(complexEmail);
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Arrange
            User user = User.builder()
                    .email("special@example.com")
                    .password("password")
                    .firstName("Jean-François")
                    .lastName("O'Brien")
                    .build();

            // Act
            User saved = userRepository.save(user);

            // Assert
            assertThat(saved.getFirstName()).isEqualTo("Jean-François");
            assertThat(saved.getLastName()).isEqualTo("O'Brien");
        }
    }
}