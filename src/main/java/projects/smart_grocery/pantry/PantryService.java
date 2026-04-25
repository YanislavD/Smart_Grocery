package projects.smart_grocery.pantry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projects.smart_grocery.household.Household;
import projects.smart_grocery.household.HouseholdService;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.pantry.dto.UpdatePantryItemRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PantryService {
    private final PantryRepository pantryRepository;
    private final ProductService productService;
    private final HouseholdService householdService;

    public List<PantryItem> getByHousehold(Long householdId) {
        return pantryRepository.findByHouseholdId(householdId);
    }

    public PantryItem create(CreatePantryItemRequest req) {
        Household household = householdService.getByIdOrThrow(req.householdId());
        Product product = productService.getByIdOrThrow(req.productId());
        validateUnitForProduct(product, req.unit());
        PantryItem item = PantryItem.builder()
                .household(household)
                .product(product)
                .qty(req.qty())
                .unit(req.unit())
                .expiryDate(req.expiryDate())
                .minQtyThreshold(req.minQtyThreshold())
                .build();
        return pantryRepository.save(item);
    }

    public PantryItem update(Long id, UpdatePantryItemRequest req) {
        PantryItem item = pantryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pantry item not found"));
        if (req.qty() != null) item.setQty(req.qty());
        if (req.unit() != null) {
            validateUnitForProduct(item.getProduct(), req.unit());
            item.setUnit(req.unit());
        }
        if (req.expiryDate() != null) item.setExpiryDate(req.expiryDate());
        if (req.minQtyThreshold() != null) item.setMinQtyThreshold(req.minQtyThreshold());
        return pantryRepository.save(item);
    }

    public void delete(Long id) {
        pantryRepository.deleteById(id);
    }

    public long countPantryItems() {
        return pantryRepository.count();
    }

    public long countPantryItemsByHousehold(Long householdId) {
        return pantryRepository.countByHouseholdId(householdId);
    }

    private void validateUnitForProduct(Product product, UnitType unit) {
        if (product.getDefaultUnit() == null) {
            return;
        }
        if (!product.getDefaultUnit().equals(unit)) {
            throw new IllegalArgumentException(
                    "Invalid unit for product '" + product.getName() + "'. Expected: " + product.getDefaultUnit()
            );
        }
    }
}
