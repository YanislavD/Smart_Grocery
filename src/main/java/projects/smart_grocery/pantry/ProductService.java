package projects.smart_grocery.pantry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product getByIdOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public long countProducts() {
        return productRepository.count();
    }

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
}
