package projects.smart_grocery.shopping;

import java.math.BigDecimal;

public record ShoppingListViewItem(
        Long id,
        Long productId,
        String productName,
        BigDecimal qty,
        boolean checked
) {
}
