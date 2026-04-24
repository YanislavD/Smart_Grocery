package projects.smart_grocery.pantry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface    PantryRepository extends JpaRepository<PantryItem,Long> {
    List<PantryItem> findByHouseholdId(Long householdId);
    long countByHouseholdId(Long householdId);

}
