package projects.smart_grocery.pantry;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    @Column(nullable = false, length = 120)

    private String name;
    @Column(length = 60)

    private String category;
    @Enumerated(EnumType.STRING)

    @Column(name = "default_unit", length = 30)
    private UnitType defaultUnit;
}
