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
    Page<PantryItem> findByHouseholdId(Long householdId, Pageable pageable);
    @Query("select p from PantryItem p where p.household.id = :householdId and p.qty <= p.minQtyThreshold")
    Page<PantryItem> findLowStockByHouseholdId(@Param("householdId") Long householdId, Pageable pageable);
    @Query("select p from PantryItem p where p.household.id = :householdId and p.expiryDate is not null and p.expiryDate <= :thresholdDate")
    Page<PantryItem> findExpiryRiskByHouseholdId(@Param("householdId") Long householdId,
                                                 @Param("thresholdDate") LocalDate thresholdDate,
                                                 Pageable pageable);
    @Query("select p from PantryItem p where p.household.id = :householdId and p.qty <= p.minQtyThreshold and p.expiryDate is not null and p.expiryDate <= :thresholdDate")
    Page<PantryItem> findLowStockAndExpiryRiskByHouseholdId(@Param("householdId") Long householdId,
                                                            @Param("thresholdDate") LocalDate thresholdDate,
                                                            Pageable pageable);
    long countByHouseholdId(Long householdId);

}
