package reserve;

import exception.WarehouseException;

// A: pure business rules. No files, no state. */
public class ShelfRules {
    public static final int STOCK_PER_SKU = 100;

    // Throws if the order is invalid or the shelf cannot cover it
    public void check(String sku, int quantity, int alreadyReserved) {
        if (sku == null || !sku.matches("[A-Za-z0-9_-]+"))
            throw new WarehouseException("Invalid SKU: " + sku);
        if (quantity < 1)
            throw new WarehouseException("Quantity must be at least 1");
        int available = STOCK_PER_SKU - alreadyReserved;
        if (quantity > available)
            throw new WarehouseException("Not enough " + sku + " on the shelf: asked " + quantity + ", available " + available);
    }
}