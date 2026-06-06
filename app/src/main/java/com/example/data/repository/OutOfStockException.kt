package com.example.data.repository

/**
 * H3 / Phase 7B-2 — Thrown by [CartRepository.addToCart] and
 * [OrderRepository.placeCODOrder] when the requested quantity
 * exceeds [com.example.data.model.Product.stock].
 *
 * The exception carries both the product id (so the UI can
 * surface it) and the current stock value (so the caller can
 * show "only N left" instead of just a generic out-of-stock
 * message — the UX matters because the user already filled in
 * the cart before discovering the stock mismatch in the
 * checkout flow).
 *
 * This is a `RuntimeException` because Room's `withTransaction`
 * doesn't propagate checked exceptions through the suspend
 * boundary cleanly, and the call site only ever needs to know
 * "this product is out of stock" — not the full cause chain.
 * The message + productId + stock are enough for every
 * consumer ([CartViewModel], [OrderViewModel]).
 */
class OutOfStockException(
    val productId: String,
    val productNameAr: String,
    val productNameEn: String,
    val availableStock: Int,
    val requestedQuantity: Int,
) : RuntimeException(
    "Out of stock for product '$productId' ($productNameAr / $productNameEn): " +
        "requested $requestedQuantity but only $availableStock available"
)
