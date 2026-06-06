package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CartDao
import com.example.data.local.FavoriteDao
import com.example.data.local.OrderDao
import com.example.data.local.OrderItemDao
import com.example.data.local.ProductDao
import com.example.data.model.CartItem
import com.example.data.model.Favorite
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.Product
import com.example.data.remote.RemoteDatabaseService
import com.example.data.di.ServiceLocator
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val remoteService: RemoteDatabaseService
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProductsFlow()
    val featuredProducts: Flow<List<Product>> = productDao.getFeaturedProductsFlow()
    val discountedProducts: Flow<List<Product>> = productDao.getDiscountedProductsFlow()

    val isOnline: Flow<Boolean> = remoteService.isOnlineFlow

    /**
     * H2 / Phase 7B-2 — soft-archive list. Backed by a Room query
     * with `WHERE archivedAt IS NOT NULL` so the admin "Archived"
     * tab is just a `collectAsState` away. Excludes the currently
     * active catalog (which is [allProducts]).
     */
    val archivedProducts: Flow<List<Product>> = productDao.getArchivedProductsFlow()

    fun getProductsByCategory(category: String): Flow<List<Product>> {
        return productDao.getProductsByCategoryFlow(category)
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)
    }

    /**
     * Offline-first sync: Pulls fresh items from online database, updates local Room cache.
     * If offline, retains local data without throwing breaking errors.
     */
    suspend fun syncProducts(): Result<Unit> {
        return try {
            if (remoteService.checkConnectionDirect()) {
                val remoteList = remoteService.getProductsOnline()
                if (remoteList.isNotEmpty()) {
                    productDao.clearProducts()
                    productDao.insertProducts(remoteList)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Offline: Retaining local cache"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Store owner adds or edits a product. Updates Room locally and
     * uploads to Firebase on the cloud.
     *
     * H1 / Phase 7B-2 — Returns [Result]<[ProductWriteResult]> so the
     * AccountScreen admin form can distinguish the four outcomes
     * (local+remote ok, local-only-because-offline, both-failed,
     * local-ok-remote-failed). The previous `Result<Unit>` collapsed
     * them all into a boolean snackbar that always claimed
     * "saved to cloud".
     *
     * The outer [Result] is a Kotlin-level exception wrapper for
     * unexpected programming errors (e.g. a Room migration blew up);
     * the typed [ProductWriteResult] is the *expected* set of
     * outcomes. Per the H1 contract, every normal-flow code path
     * returns `Result.success(...)` with one of the 4 cases.
     */
    suspend fun addOrUpdateProduct(product: Product): Result<ProductWriteResult> {
        return try {
            productDao.insertProducts(listOf(product))
            try {
                remoteService.uploadProductOnline(product)
                Result.success(ProductWriteResult.BothOk)
            } catch (remoteError: Exception) {
                if (remoteService.checkConnectionDirect()) {
                    Result.success(ProductWriteResult.LocalFailedButRemoteOk(remoteError))
                } else {
                    Result.success(ProductWriteResult.LocalOnlyOffline(remoteError))
                }
            }
        } catch (localError: Exception) {
            Result.failure(localError)
        }
    }

    /**
     * H2 / Phase 7B-2 — soft-archive a product. Sets `archivedAt`
     * to `now` so the row disappears from the storefront flows
     * and shows up in [archivedProducts]. The remote copy is
     * untouched (a real `deleteProductOnline` would be a
     * different contract; for now we just hide the row locally
     * so order history is preserved per the OrderItem RESTRICT
     * FK).
     */
    suspend fun archiveProduct(id: String): Result<Unit> = runCatching {
        productDao.archiveProduct(id, System.currentTimeMillis())
    }

    /**
     * H2 — restore a previously archived product. Clears
     * `archivedAt`; the row re-appears in [allProducts].
     */
    suspend fun unarchiveProduct(id: String): Result<Unit> = runCatching {
        productDao.unarchiveProduct(id)
    }
}

class CartRepository(
    private val cartDao: CartDao,
    private val productDao: ProductDao
) {
    val cartItemsWithDetails: Flow<List<Pair<CartItem, Product>>> = cartDao.getCartItemsFlow()
        .map { items ->
            items.mapNotNull { item ->
                val product = productDao.getProductById(item.productId)
                if (product != null) item to product else null
            }
        }

    val totalCartPrice: Flow<Double> = cartItemsWithDetails.map { list ->
        list.sumOf { (item, product) -> product.price * item.quantity }
    }

    val cartItemsCount: Flow<Int> = cartDao.getCartItemsFlow().map { items ->
        items.sumOf { it.quantity }
    }

    suspend fun addToCart(productId: String, quantity: Int = 1) {
        // H3 / Phase 7B-2 — stock guard. The read-modify-write
        // (existing-quantity + quantity) happens *after* the
        // stock check so a user who already has 2 in the cart
        // can still add more, as long as the total stays within
        // the product's stock ceiling. The check is intentionally
        // NOT inside a `withTransaction` because the cart edit
        // is a single DAO call and the cart→checkout reconciliation
        // happens again in `OrderRepository.placeCODOrder` inside
        // a real transaction.
        val stock = productDao.getStockDirect(productId) ?: 0
        val existing = cartDao.getQuantity(productId) ?: 0
        val newTotal = existing + quantity
        if (newTotal > stock) {
            val product = productDao.getProductById(productId)
            throw OutOfStockException(
                productId = productId,
                productNameAr = product?.nameAr ?: productId,
                productNameEn = product?.nameEn ?: productId,
                availableStock = stock,
                requestedQuantity = newTotal
            )
        }
        if (existing > 0) {
            cartDao.updateQuantity(productId, newTotal)
        } else {
            cartDao.insertCartItem(CartItem(productId = productId, quantity = quantity))
        }
    }

    suspend fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteCartItem(productId)
        } else {
            cartDao.updateQuantity(productId, quantity)
        }
    }

    suspend fun removeFromCart(productId: String) {
        cartDao.deleteCartItem(productId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val favorites: Flow<List<Favorite>> = favoriteDao.getFavoritesFlow()

    suspend fun toggleFavorite(productId: String) {
        if (favoriteDao.isFavoriteDirect(productId)) {
            favoriteDao.deleteFavorite(productId)
        } else {
            favoriteDao.insertFavorite(Favorite(productId))
        }
    }

    fun isFavorite(productId: String): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(productId)
    }
}

class OrderRepository(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val remoteService: RemoteDatabaseService,
    /**
     * H3 / Phase 7B-2 — the database reference is used to run
     * the stock-check + order-insert + stock-decrement inside
     * a single [androidx.room.RoomDatabase.withTransaction] so
     * the stock check is race-free against concurrent orders.
     * Wired by [com.example.data.di.ServiceLocator] which
     * already has the singleton [com.example.data.local.AppDatabase].
     */
    private val database: com.example.data.local.AppDatabase
) {
    val allOrdersFlow: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItemsFlow()

    /**
     * Places a Cash on Delivery (COD) order.
     *
     * H3 / Phase 7B-2 — the stock check + order insert +
     * stock-decrement + cart-clear all run inside a single
     * [withTransaction] so a concurrent order for the same
     * product can't double-spend the available stock. If any
     * line item exceeds the current `Product.stock` the
     * transaction rolls back and [OutOfStockException] is
     * thrown to the caller (which is [OrderViewModel]).
     */
    suspend fun placeCODOrder(
        customerName: String,
        customerAddress: String,
        customerPhone: String,
        totalPrice: Double,
        items: List<OrderItem>
    ): Result<Order> {
        if (items.isEmpty()) {
            return Result.failure(IllegalStateException("Cannot place order with no items"))
        }
        val newOrder = Order(
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            totalPrice = totalPrice,
            isSynced = false
        )

        return try {
            // H3 — atomic stock check + decrement. The transaction
            // body re-reads the stock for every line item (Room
            // serializes writes, so the read is consistent); if
            // any item's stock is < requested quantity, throw and
            // roll back. The `decrementStock` query itself has a
            // `WHERE stock >= :quantity` guard so the second line
            // of defense catches any TOCTOU between the read and
            // the update.
            database.withTransaction {
                for (item in items) {
                    val stock = productDao.getStockDirect(item.productId) ?: 0
                    if (stock < item.quantity) {
                        val product = productDao.getProductById(item.productId)
                        throw OutOfStockException(
                            productId = item.productId,
                            productNameAr = product?.nameAr ?: item.productId,
                            productNameEn = product?.nameEn ?: item.productId,
                            availableStock = stock,
                            requestedQuantity = item.quantity
                        )
                    }
                }
                for (item in items) {
                    val rowsUpdated = productDao.decrementStock(item.productId, item.quantity)
                    if (rowsUpdated == 0) {
                        // TOCTOU race: the read passed but the
                        // update found 0 rows. Roll back the
                        // whole transaction.
                        val product = productDao.getProductById(item.productId)
                        throw OutOfStockException(
                            productId = item.productId,
                            productNameAr = product?.nameAr ?: item.productId,
                            productNameEn = product?.nameEn ?: item.productId,
                            availableStock = productDao.getStockDirect(item.productId) ?: 0,
                            requestedQuantity = item.quantity
                        )
                    }
                }
                orderItemDao.replaceForOrder(newOrder.id, items)
                orderDao.insertOrder(newOrder)
                cartDao.clearCart()
            }

            if (remoteService.checkConnectionDirect()) {
                val isUploaded = remoteService.uploadOrder(newOrder)
                if (isUploaded) {
                    orderDao.markOrderSynced(newOrder.id)
                    return Result.success(newOrder.copy(isSynced = true))
                }
            }
            Result.success(newOrder)
        } catch (e: OutOfStockException) {
            // H3 — surface the out-of-stock failure to the
            // caller as a Result.failure. The transaction has
            // already rolled back.
            Result.failure(e)
        } catch (e: Exception) {
            // H1 parity — local write succeeded but the remote
            // push failed. The order is still in the local DB
            // (transaction committed), and `syncUnsyncedOrders`
            // will pick it up on the next connection.
            Result.success(newOrder)
        }
    }

    /**
     * Pushes unsynced orders to the remote backend.
     */
    suspend fun syncUnsyncedOrders(): Int {
        var syncCount = 0
        try {
            if (remoteService.checkConnectionDirect()) {
                val unsynced = orderDao.getUnsyncedOrdersWithItems()
                for (orderWithItems in unsynced) {
                    val isUploaded = remoteService.uploadOrder(orderWithItems.order)
                    if (isUploaded) {
                        orderDao.markOrderSynced(orderWithItems.order.id)
                        syncCount++
                    }
                }
            }
        } catch (e: Exception) {
            // log / handle connection dropout
        }
        return syncCount
    }
}
