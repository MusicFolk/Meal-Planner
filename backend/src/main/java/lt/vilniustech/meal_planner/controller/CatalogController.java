package lt.vilniustech.meal_planner.controller;

import java.util.List;
import lt.vilniustech.meal_planner.dto.CatalogDtos.AllergenDto;
import lt.vilniustech.meal_planner.dto.CatalogDtos.ProductDto;
import lt.vilniustech.meal_planner.service.IProductCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final IProductCatalog catalog;

    public CatalogController(IProductCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/allergens")
    public List<AllergenDto> allergens() {
        return catalog.allAllergens().stream().map(AllergenDto::of).toList();
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return catalog.allProducts().stream().map(ProductDto::of).toList();
    }
}
