package org.learnbudget.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.learnbudget.model.BudgetAllocation;
import org.learnbudget.model.enums.BudgetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {
        private String name;
        private BudgetType type;
        private BigDecimal totalAmount;
        private List<BudgetAllocation> allocations = new ArrayList<>();
}
