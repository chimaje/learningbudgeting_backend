package org.learnbudget.repository;


import org.learnbudget.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

       List<Budget> findAllBudgets();
       List<Budget> findActiveBudgets();
       Optional<Budget> findUniqueBudget();
       List<Budget>  findBudgetByType();
       List<Budget> findByBudgetbyDateRange();
       void deleteByUserIdAndId();

       // Check if user owns a budget (for authorization)
       boolean existsByUserIdAndId();

}
