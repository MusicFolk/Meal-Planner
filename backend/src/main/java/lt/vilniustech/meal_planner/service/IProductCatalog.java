package lt.vilniustech.meal_planner.service;

import java.util.Collection;
import java.util.List;
import lt.vilniustech.meal_planner.entity.Allergen;
import lt.vilniustech.meal_planner.entity.Product;

/** Produktų ir maistinės vertės komponento (PRD) sąsaja. */
public interface IProductCatalog {

    List<Product> allProducts();

    List<Allergen> allAllergens();

    List<Product> productsByIds(Collection<Long> ids);

    List<Allergen> allergensByIds(Collection<Long> ids);

    /** Leistini produktai: be produktų su kliento alergenais ir be nemėgstamų produktų (PRD-2). */
    List<Product> allowedProducts(Collection<Allergen> allergens, Collection<Product> dislikedProducts);
}
