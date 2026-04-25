package projects.smart_grocery.pantry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projects.smart_grocery.household.Household;
import projects.smart_grocery.household.HouseholdService;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.pantry.dto.UpdatePantryItemRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PantryServiceTest {

    @Mock
    private PantryRepository pantryRepository;
    @Mock
    private ProductService productService;
    @Mock
    private HouseholdService householdService;

    @InjectMocks
    private PantryService pantryService;

    @Test
    void create_throwsWhenUnitDoesNotMatchProductDefaultUnit() {
        Household household = Household.builder().id(10L).name("Home").ownerId(1L).build();
        Product milk = Product.builder().id(3L).name("Milk").defaultUnit(UnitType.L).build();
        CreatePantryItemRequest request = new CreatePantryItemRequest(
                10L, 3L, new BigDecimal("2.00"), UnitType.KG, null, new BigDecimal("1.00")
        );

        when(householdService.getByIdOrThrow(10L)).thenReturn(household);
        when(productService.getByIdOrThrow(3L)).thenReturn(milk);

        assertThrows(IllegalArgumentException.class, () -> pantryService.create(request));
    }

    @Test
    void deleteForHousehold_throwsWhenItemBelongsToAnotherHousehold() {
        Household anotherHousehold = Household.builder().id(200L).name("Other").ownerId(2L).build();
        PantryItem item = PantryItem.builder().id(5L).household(anotherHousehold).build();
        when(pantryRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> pantryService.deleteForHousehold(5L, 100L));
    }

    @Test
    void updateForHousehold_allowsClearingExpiryDate() {
        Household household = Household.builder().id(100L).name("Home").ownerId(1L).build();
        Product rice = Product.builder().id(4L).name("Rice").defaultUnit(UnitType.KG).build();
        PantryItem item = PantryItem.builder()
                .id(7L)
                .household(household)
                .product(rice)
                .expiryDate(LocalDate.of(2026, 12, 1))
                .qty(new BigDecimal("2.0"))
                .unit(UnitType.KG)
                .minQtyThreshold(new BigDecimal("1.0"))
                .build();
        when(pantryRepository.findById(7L)).thenReturn(Optional.of(item));
        when(pantryRepository.save(any(PantryItem.class))).thenReturn(item);

        pantryService.updateForHousehold(7L, 100L,
                new UpdatePantryItemRequest(new BigDecimal("2.2"), UnitType.KG, null, new BigDecimal("1.1")));

        assertTrue(item.getExpiryDate() == null);
        verify(pantryRepository).save(item);
    }

    @Test
    void isLowStock_returnsExpectedResult() {
        PantryItem low = PantryItem.builder()
                .qty(new BigDecimal("1.00"))
                .minQtyThreshold(new BigDecimal("1.50"))
                .build();
        PantryItem ok = PantryItem.builder()
                .qty(new BigDecimal("2.00"))
                .minQtyThreshold(new BigDecimal("1.50"))
                .build();

        assertTrue(pantryService.isLowStock(low));
        assertFalse(pantryService.isLowStock(ok));
    }
}
