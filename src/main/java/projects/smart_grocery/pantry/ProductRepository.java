package projects.smart_grocery.pantry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {
    @Query("select distinct p.category from Product p where p.category is not null and p.category <> '' order by p.category asc")
    List<String> findDistinctCategories();
}
