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
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE isFeatured = 1")
    fun getFeaturedProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDiscounted = 1")
    fun getDiscountedProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE categoryAr = :category OR categoryEn = :category")
    fun getProductsByCategoryFlow(category: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()
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
    version = 2,
    exportSchema = false
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
