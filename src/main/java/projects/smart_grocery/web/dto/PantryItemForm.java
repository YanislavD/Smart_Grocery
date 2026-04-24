package projects.smart_grocery.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import projects.smart_grocery.pantry.UnitType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PantryItemForm {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal qty;

    @NotNull
    private UnitType unit;

    private LocalDate expiryDate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal minQtyThreshold;
}
