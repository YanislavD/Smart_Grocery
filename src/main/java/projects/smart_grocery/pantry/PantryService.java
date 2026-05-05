package projects.smart_grocery.pantry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import projects.smart_grocery.household.Household;
import projects.smart_grocery.household.HouseholdService;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.pantry.dto.UpdatePantryItemRequest;

import java.time.LocalDate;
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

    public Page<PantryItem> getByHouseholdPaged(Long householdId,
                                                boolean lowStockOnly,
                                                boolean expiringSoonOnly,
                                                int expiryWarningDays,
                                                String category,
                                                Pageable pageable) {
        LocalDate thresholdDate = LocalDate.now().plusDays(expiryWarningDays);
        return pantryRepository.findByHouseholdWithFilters(
                householdId,
                lowStockOnly,
                expiringSoonOnly,
                thresholdDate,
                category,
                pageable
        );
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

    public PantryItem updateForHousehold(Long id, Long householdId, UpdatePantryItemRequest req) {
        PantryItem item = pantryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pantry item not found"));
        if (!item.getHousehold().getId().equals(householdId)) {
            throw new IllegalArgumentException("Pantry item does not belong to your household");
        }
        if (req.qty() != null) item.setQty(req.qty());
        if (req.unit() != null) {
            validateUnitForProduct(item.getProduct(), req.unit());
            item.setUnit(req.unit());
        }
        item.setExpiryDate(req.expiryDate());
        if (req.minQtyThreshold() != null) item.setMinQtyThreshold(req.minQtyThreshold());
        return pantryRepository.save(item);
    }

    public void delete(Long id) {
        pantryRepository.deleteById(id);
    }

    public void deleteForHousehold(Long id, Long householdId) {
        PantryItem item = pantryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pantry item not found"));
        if (!item.getHousehold().getId().equals(householdId)) {
            throw new IllegalArgumentException("Pantry item does not belong to your household");
        }
        pantryRepository.delete(item);
    }

    public long countPantryItems() {
        return pantryRepository.count();
    }

    public long countPantryItemsByHousehold(Long householdId) {
        return pantryRepository.countByHouseholdId(householdId);
    }

    public boolean isLowStock(PantryItem item) {
        if (item.getQty() == null || item.getMinQtyThreshold() == null) {
            return false;
        }
        return item.getQty().compareTo(item.getMinQtyThreshold()) <= 0;
    }

    public boolean isExpiringSoon(PantryItem item, int withinDays) {
        if (item.getExpiryDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(withinDays);
        // "Expiring soon" bucket includes already expired items,
        // so users can see all expiry-related risks in one place.
        return !item.getExpiryDate().isAfter(threshold);
    }

    public boolean isExpired(PantryItem item) {
        if (item.getExpiryDate() == null) {
            return false;
        }
        return item.getExpiryDate().isBefore(LocalDate.now());
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
