package projects.smart_grocery.pantry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface    PantryRepository extends JpaRepository<PantryItem,Long> {
    List<PantryItem> findByHouseholdId(Long householdId);
    @Query("""
           select p
           from PantryItem p
           where p.household.id = :householdId
             and (:lowStockOnly = false or p.qty <= p.minQtyThreshold)
             and (:expiringSoonOnly = false or (p.expiryDate is not null and p.expiryDate <= :thresholdDate))
             and (:category is null or :category = '' or lower(p.product.category) = lower(:category))
           """)
    Page<PantryItem> findByHouseholdWithFilters(@Param("householdId") Long householdId,
                                                @Param("lowStockOnly") boolean lowStockOnly,
                                                @Param("expiringSoonOnly") boolean expiringSoonOnly,
                                                @Param("thresholdDate") LocalDate thresholdDate,
                                                @Param("category") String category,
                                                Pageable pageable);
    long countByHouseholdId(Long householdId);

}
