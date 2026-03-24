package org.learnbudget.repository;

import org.learnbudget.model.Category;

import java.util.List;

public interface CategoryRepository {
    // Find all categories for a specific user
    List<Category> findByUserId(Long userId);
    // Check if category exists for user
    boolean existsByUserIdAndName(Long userId, String name);

    // Check if user owns category
    boolean existsByUserIdAndId(Long userId, Long categoryId);
}
