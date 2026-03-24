package org.learnbudget.repository;

import org.learnbudget.model.BudgetAllocation;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BudgetAllocationRepository {
    // Find all allocations for a specific budget
    List<BudgetAllocation> findByBudgetId(Long budgetId);

    // Find specific allocation
    Optional<BudgetAllocation> findByBudgetIdAndCategoryId(Long budgetId, Long categoryId);

    // Check if allocation exists
    boolean existsByBudgetIdAndCategoryId(Long budgetId, Long categoryId);

    // Check if category is allocated to any budget
    boolean existsByCategoryId(Long categoryId);

    // Get allocations for a budget with category details (join fetch)
    @Query("SELECT ba FROM BudgetAllocation ba " +
            "JOIN FETCH ba.budget b " +
            "JOIN FETCH ba.category c " +
            "WHERE b.id = :budgetId")
    List<BudgetAllocation> findByBudgetIdWithDetails(Long budgetId);

    // Sum of spent amounts for a budget
    @Query("SELECT COALESCE(SUM(ba.spentAmount), 0) FROM BudgetAllocation ba " +
            "WHERE ba.budgetId = :budgetId")
    BigDecimal sumSpentAmountByBudgetId(Long budgetId);

    // Delete all allocations for a budget
    void deleteByBudgetId(Long budgetId);

    // Count allocations for a category
    Long countByCategoryId(Long categoryId);
}
