package com.example.data.remote

import com.example.data.model.Order
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Common Remote Service Contract.
 * Highly scalable: to migrate of Firebase to Supabase or custom HTTP/GraphQL backends,
 * simply implement this contract and supply it to the repositories.
 */
interface RemoteDatabaseService {
    /**
     * Fetch products from the online database.
     */
    suspend fun getProductsOnline(): List<Product>

    /**
     * Send orders to the online shop database.
     * Cash-On-Delivery orders will sync automatically with the customer details.
     */
    suspend fun uploadOrder(order: Order): Boolean

    /**
     * Send new/edited product details to the online database to modify inventory.
     */
    suspend fun uploadProductOnline(product: Product): Boolean

    /**
     * Send or synchronize customer profile info directly onto the Firebase/cloud system.
     */
    suspend fun uploadUserProfileOnline(
        username: String,
        phone: String,
        countryCode: String,
        location: String,
        avatarIndex: Int
    ): Boolean

    /**
     * Get list of synchronized customer profiles (used by Admin Panel to browse customers).
     */
    suspend fun getCustomersOnline(): List<com.example.data.model.UserProfile>

    /**
     * Flow tracking whether the system can communicate with the remote server.
     */
    val isOnlineFlow: Flow<Boolean>

    /**
     * Direct sync check or manual trigger.
     */
    suspend fun checkConnectionDirect(): Boolean
}
