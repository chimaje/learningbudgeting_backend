package org.learnbudget.repository;

import jakarta.transaction.Transactional;
import org.learnbudget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find users by first name (case-insensitive)
     * @return list of users with matching first name
     */
    List<User> findByFirstNameIgnoreCase(String firstName);

    /**
     * Find users by last name (case-insensitive)
     * @return list of users with matching last name
     */
    List<User> findByLastNameIgnoreCase(String lastName);

    /**
     * Find users created after a specific date
     * @return list of users created after the date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users created between two dates
     * @return list of users created in the date range
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Delete a user by email address
     * @param email the email of the user to delete
     */
    void deleteByEmail(String email);

    /**
     * Count total number of users in the system
     * @return total user count
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    /**
     * Find users with budgets (users who have at least one budget)
     * @return list of users with budgets
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.budgets b")
    List<User> findUsersWithBudgets();

    /**
     * Find users without any budgets
     * @return list of users without budgets
     */
    @Query("SELECT u FROM User u WHERE u.budgets IS EMPTY")
    List<User> findUsersWithoutBudgets();

    /**
     * Update user's last name
     * @param email user's email
     * @param lastName new last name
     */
    @Transactional
    @Modifying // this tells the system this is a query that is going to make a change or changes  to a piece of  data in the database
    @Query("UPDATE User u SET u.lastName = :lastName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.email = :email")
    void updateLastNameByEmail(@Param("email") String email, @Param("lastName") String lastName);
}