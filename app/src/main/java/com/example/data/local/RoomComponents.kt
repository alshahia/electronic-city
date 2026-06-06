package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.data.model.CartItem
import com.example.data.model.Favorite
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /**
     * H2 / Phase 7B-2 — storefront reads only see non-archived
     * products. The `ORDER BY id DESC` is preserved from the
     * pre-archive query so the home / products tabs look the same
     * to existing users.
     */
    @Query("SELECT * FROM products WHERE archivedAt IS NULL ORDER BY id DESC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE archivedAt IS NULL")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE isFeatured = 1 AND archivedAt IS NULL")
    fun getFeaturedProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDiscounted = 1 AND archivedAt IS NULL")
    fun getDiscountedProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE (categoryAr = :category OR categoryEn = :category) AND archivedAt IS NULL")
    fun getProductsByCategoryFlow(category: String): Flow<List<Product>>

    /**
     * H2 — admin "Archived" tab. Newest archive first so the most
     * recently soft-deleted items surface at the top of the list.
     */
    @Query("SELECT * FROM products WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun getArchivedProductsFlow(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    /**
     * H2 — mark a product as archived. We pass the timestamp in
     * from the VM so the test (MigrationTest) can pin it
     * deterministically and so the recorded time matches the
     * "Archived" list ordering.
     */
    @Query("UPDATE products SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveProduct(id: String, archivedAt: Long)

    @Query("UPDATE products SET archivedAt = NULL WHERE id = :id")
    suspend fun unarchiveProduct(id: String)

    /**
     * H3 / Phase 7B-2 — single-shot stock read for the cart /
     * order path. Returns `null` when the product is missing
     * (deleted or never existed). Used by
     * [com.example.data.repository.CartRepository.addToCart]
     * to throw [com.example.data.repository.OutOfStockException]
     * before mutating the cart.
     */
    @Query("SELECT stock FROM products WHERE id = :id")
    suspend fun getStockDirect(id: String): Int?

    /**
     * H3 — decrement stock for a placed order. The caller is
     * expected to have already verified the row exists and the
     * quantity fits; Room's `UPDATE … WHERE stock >= :quantity`
     * is the second line of defense (returns 0 rows affected
     * if the stock changed between the read and the write, so
     * the transaction aborts cleanly).
     */
    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    suspend fun decrementStock(id: String, quantity: Int): Int

    /**
     * H3 — restore stock when an order is cancelled or a
     * cart-edit rolls the user's quantity back below the prior
     * committed value. Symmetric to [decrementStock] but
     * without the `>= :quantity` guard (adding stock back is
     * always safe).
     */
    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :id")
    suspend fun restoreStock(id: String, quantity: Int): Int
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY dateAdded DESC")
    fun getCartItemsFlow(): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items")
    suspend fun getCartItems(): List<CartItem>

    @Query("SELECT quantity FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getQuantity(productId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE productId = :productId")
    suspend fun updateQuantity(productId: String, quantity: Int)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    fun getFavoritesFlow(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites")
    suspend fun getFavorites(): List<Favorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE productId = :productId")
    suspend fun deleteFavorite(productId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    fun isFavoriteFlow(productId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    suspend fun isFavoriteDirect(productId: String): Boolean
}

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersWithItemsFlow(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrdersWithItems(): List<OrderWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Query("UPDATE orders SET isSynced = 1 WHERE id = :orderId")
    suspend fun markOrderSynced(orderId: String)
}

@Dao
interface OrderItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    @Transaction
    suspend fun replaceForOrder(orderId: String, items: List<OrderItem>) {
        deleteItemsForOrder(orderId)
        if (items.isNotEmpty()) insertOrderItems(items)
    }
}

@Database(
    entities = [Product::class, CartItem::class, Favorite::class, Order::class, OrderItem::class],
    /**
     * H2 / Phase 7B-2 — bumped 2 → 3 to add `archivedAt: Long?` to
     * the `Product` entity. MIGRATION_2_3 in `Migrations.kt`
     * handles the ALTER TABLE for existing v2 users; the schema is
     * exported under `app/schemas/` so Robolectric can boot a
     * v2 snapshot in `MigrationTest`.
     */
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "electronic_city_db"
                )
                    .addMigrations(com.example.data.local.MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
