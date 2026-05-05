package projects.smart_grocery.shopping;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projects.smart_grocery.pantry.PantryItem;
import projects.smart_grocery.pantry.PantryService;
import projects.smart_grocery.pantry.Product;
import projects.smart_grocery.pantry.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingListService {
    private static final String OPEN_STATUS = "OPEN";

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final PantryService pantryService;
    private final ProductService productService;

    public ShoppingList getOrCreateOpenList(Long householdId) {
        return shoppingListRepository
                .findFirstByHouseholdIdAndStatusOrderByCreatedAtDesc(householdId, OPEN_STATUS)
                .orElseGet(() -> shoppingListRepository.save(ShoppingList.builder()
                        .householdId(householdId)
                        .status(OPEN_STATUS)
                        .build()));
    }

    public void generateFromLowStock(Long householdId) {
        ShoppingList list = getOrCreateOpenList(householdId);
        shoppingListItemRepository.deleteByShoppingListId(list.getId());

        List<PantryItem> lowStockItems = pantryService.getByHousehold(householdId).stream()
                .filter(pantryService::isLowStock)
                .toList();

        for (PantryItem pantryItem : lowStockItems) {
            BigDecimal qty = suggestedQty(pantryItem);
            shoppingListItemRepository.save(ShoppingListItem.builder()
                    .shoppingListId(list.getId())
                    .productId(pantryItem.getProduct().getId())
                    .qty(qty)
                    .checked(false)
                    .build());
        }
    }

    public List<ShoppingListViewItem> getViewItemsForHousehold(Long householdId) {
        ShoppingList list = getOrCreateOpenList(householdId);
        List<ShoppingListItem> items = shoppingListItemRepository.findByShoppingListIdOrderByIdAsc(list.getId());

        Map<Long, Product> productsById = productService.findAllProducts().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> new ShoppingListViewItem(
                        item.getId(),
                        item.getProductId(),
                        productsById.containsKey(item.getProductId()) ? productsById.get(item.getProductId()).getName() : "Unknown product",
                        item.getQty(),
                        item.isChecked()
                ))
                .toList();
    }

    public void updateItemQty(Long householdId, Long itemId, BigDecimal qty) {
        ShoppingListItem item = getItemForHousehold(householdId, itemId);
        item.setQty(qty);
        shoppingListItemRepository.save(item);
    }

    public void markItemChecked(Long householdId, Long itemId, boolean checked) {
        ShoppingListItem item = getItemForHousehold(householdId, itemId);
        item.setChecked(checked);
        shoppingListItemRepository.save(item);
    }

    public void removeItem(Long householdId, Long itemId) {
        ShoppingListItem item = getItemForHousehold(householdId, itemId);
        shoppingListItemRepository.delete(item);
    }

    private ShoppingListItem getItemForHousehold(Long householdId, Long itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Shopping list item not found"));
        ShoppingList list = shoppingListRepository.findById(item.getShoppingListId())
                .orElseThrow(() -> new IllegalArgumentException("Shopping list not found"));
        if (!list.getHouseholdId().equals(householdId)) {
            throw new IllegalArgumentException("Shopping list item does not belong to your household");
        }
        return item;
    }

    private BigDecimal suggestedQty(PantryItem pantryItem) {
        if (pantryItem.getQty() == null || pantryItem.getMinQtyThreshold() == null) {
            return BigDecimal.ONE;
        }
        BigDecimal delta = pantryItem.getMinQtyThreshold().subtract(pantryItem.getQty());
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return delta.stripTrailingZeros();
    }
}
