package org.learnbudget.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.learnbudget.model.BudgetAllocation;
import org.learnbudget.model.enums.BudgetType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBudgetRequest {

    @NotBlank(message = "Email is required")
    private String name;
    @NotBlank(message = "Type not Selected")
    private BudgetType type;
    @NotBlank(message = "Amount Required")
    private BigDecimal totalAmount;
    @NotBlank(message = "Email is required")
    private List<BudgetAllocation> allocations = new ArrayList<>();

}
