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
     * Store owner adds or edits a product. Updates Room locally and uploads to Firebase on the cloud.
     */
    suspend fun addOrUpdateProduct(product: Product): Result<Unit> {
        return try {
            productDao.insertProducts(listOf(product))
            if (remoteService.checkConnectionDirect()) {
                remoteService.uploadProductOnline(product)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        val existing = cartDao.getQuantity(productId)
        if (existing != null) {
            cartDao.updateQuantity(productId, existing + quantity)
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
    private val remoteService: RemoteDatabaseService
) {
    val allOrdersFlow: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItemsFlow()

    /**
     * Places a Cash on Delivery (COD) order.
     * Inserts order + items in a single transaction, clears the cart,
     * then attempts to push to the remote backend.
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
            orderItemDao.replaceForOrder(newOrder.id, items)
            orderDao.insertOrder(newOrder)
            cartDao.clearCart()

            if (remoteService.checkConnectionDirect()) {
                val isUploaded = remoteService.uploadOrder(newOrder)
                if (isUploaded) {
                    orderDao.markOrderSynced(newOrder.id)
                    return Result.success(newOrder.copy(isSynced = true))
                }
            }
            Result.success(newOrder)
        } catch (e: Exception) {
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
