package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val imageUrl: String,
    val price: Double, // in IQD, e.g., 36000.0
    val originalPrice: Double? = null, // for discounts
    val categoryAr: String,
    val categoryEn: String,
    val isFeatured: Boolean = false,
    val isDiscounted: Boolean = false,
    val stock: Int = 10,
    val lastUpdated: Long = System.currentTimeMillis(),
    /**
     * H2 / Phase 7B-2 — soft-archive timestamp.
     * `null` = active product, non-null = archived at this epoch ms.
     * The default keeps v0/v1/v2 builds forward-compatible: the
     * MIGRATION_2_3 adds the column as `INTEGER` (nullable by
     * definition in SQLite), so existing rows just get `null`.
     * OrderItem FK is RESTRICT (lines 71-76), so an archived
     * product is safe to keep around for order history.
     */
    val archivedAt: Long? = null
) {
    fun getName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
    fun getDescription(isArabic: Boolean): String = if (isArabic) descriptionAr else descriptionEn
    fun getCategory(isArabic: Boolean): String = if (isArabic) categoryAr else categoryEn
}

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val productId: String,
    val quantity: Int,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val productId: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val totalPrice: Double,
    val paymentMethod: String = "COD",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false, // Sync status with Firebase/Supabase
    val statusAr: String = "قيد الانتظار",
    val statusEn: String = "Pending"
) {
    fun getStatus(isArabic: Boolean): String = if (isArabic) statusAr else statusEn
}

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val productId: String,
    val nameAr: String,
    val nameEn: String,
    val price: Double,
    val quantity: Int
) {
    fun getName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)

data class UserProfile(
    val id: String,
    val username: String,
    val phone: String,
    val countryCode: String,
    val location: String,
    val avatarIndex: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_COUNTRY_CODE: String = "+964"
    }
}

