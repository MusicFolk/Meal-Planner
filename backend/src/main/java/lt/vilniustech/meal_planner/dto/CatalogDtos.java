package lt.vilniustech.meal_planner.dto;

import lt.vilniustech.meal_planner.entity.Allergen;
import lt.vilniustech.meal_planner.entity.Product;
import lt.vilniustech.meal_planner.enums.ProductCategory;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record AllergenDto(Long id, String code, String name) {

        public static AllergenDto of(Allergen a) {
            return new AllergenDto(a.getId(), a.getCode(), a.getName());
        }
    }

    public record ProductDto(Long id, String name, ProductCategory category) {

        public static ProductDto of(Product p) {
            return new ProductDto(p.getId(), p.getName(), p.getCategory());
        }
    }
}
