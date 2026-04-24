package projects.smart_grocery.pantry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import projects.smart_grocery.pantry.UnitType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePantryItemRequest (
        @NotNull Long householdId,
        @NotNull Long productId,
        @NotNull @DecimalMin("0.01") BigDecimal qty,
        @NotNull UnitType unit,
        LocalDate expiryDate,
        @NotNull @DecimalMin("0.00") BigDecimal minQtyThreshold
){}
