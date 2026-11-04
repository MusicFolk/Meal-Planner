package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Susumuotas vieno produkto kiekis gramais ir požymis, ar produktas nupirktas (FR-19, FR-20). */
@Entity
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id")
    private ShoppingList shoppingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int totalGrams;

    private boolean purchased;

    protected ShoppingListItem() {
    }

    ShoppingListItem(ShoppingList shoppingList, Product product, int totalGrams) {
        this.shoppingList = shoppingList;
        this.product = product;
        this.totalGrams = totalGrams;
    }

    public Long getId() {
        return id;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public Product getProduct() {
        return product;
    }

    public int getTotalGrams() {
        return totalGrams;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
}
