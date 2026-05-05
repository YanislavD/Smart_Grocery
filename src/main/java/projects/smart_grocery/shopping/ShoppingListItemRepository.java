package projects.smart_grocery.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
    List<ShoppingListItem> findByShoppingListIdOrderByIdAsc(Long shoppingListId);
    Optional<ShoppingListItem> findFirstByShoppingListIdAndProductId(Long shoppingListId, Long productId);
    void deleteByShoppingListId(Long shoppingListId);
}
