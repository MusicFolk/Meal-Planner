package lt.vilniustech.meal_planner.service;

import java.util.Collection;
import java.util.List;
import lt.vilniustech.meal_planner.entity.Allergen;
import lt.vilniustech.meal_planner.entity.Product;
import lt.vilniustech.meal_planner.repository.AllergenRepository;
import lt.vilniustech.meal_planner.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductCatalogService implements IProductCatalog {

    private final ProductRepository products;
    private final AllergenRepository allergens;

    public ProductCatalogService(ProductRepository products, AllergenRepository allergens) {
        this.products = products;
        this.allergens = allergens;
    }

    @Override
    public List<Product> allProducts() {
        return products.findAllByOrderByNameAsc();
    }

    @Override
    public List<Allergen> allAllergens() {
        return allergens.findAllByOrderByIdAsc();
    }

    @Override
    public List<Product> productsByIds(Collection<Long> ids) {
        return products.findAllById(ids);
    }

    @Override
    public List<Allergen> allergensByIds(Collection<Long> ids) {
        return allergens.findAllById(ids);
    }
}
