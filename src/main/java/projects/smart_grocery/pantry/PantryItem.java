package projects.smart_grocery.pantry;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pantry_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "household_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Household household;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal qty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnitType unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "min_qty_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal minQtyThreshold;
}
