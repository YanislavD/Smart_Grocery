package projects.smart_grocery.pantry.dto;

import jakarta.validation.constraints.DecimalMin;
import projects.smart_grocery.pantry.UnitType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePantryItemRequest(
        BigDecimal qty,
        UnitType unit,
        LocalDate expiryDate,
        @DecimalMin("0.00") BigDecimal minQtyThreshold
) {}
