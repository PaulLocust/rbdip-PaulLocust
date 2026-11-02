package com.rbdip.bookstore.purchase;

/** Public contract shared by consumers of purchase history. */
public interface PurchaseHistory {
    boolean hasPurchased(Long productId, String customerName);
}
