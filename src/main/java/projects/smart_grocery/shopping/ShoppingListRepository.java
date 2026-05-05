package projects.smart_grocery.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    Optional<ShoppingList> findFirstByHouseholdIdAndStatusOrderByCreatedAtDesc(Long householdId, String status);
}
