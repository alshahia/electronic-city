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

    /**
     * D9.4 — Persist the device's FCM token with the current user so
     * the backend can target this device for push notifications
     * (order updates, promotions, etc.). The in-memory implementation
     * is a no-op; the Firestore implementation writes the token to
     * `users/{uid}/devices/{tokenId}`.
     */
    suspend fun uploadDeviceToken(token: String): Boolean

    /**
     * D8.23 / Phase 7B-2 — Authorize the current caller as the store
     * admin. Used by `AdminAuthViewModel.signInWithFirebase(password)`
     * to gate the admin tabs (replaces the hardcoded `admin123`
     * check that lived in `AccountScreen.kt:896`).
     *
     * The in-memory implementation is a stub that returns
     * `Result.success(Unit)` for *any* non-blank password (kept loose
     * to preserve the demo UX). The real implementation will check a
     * Firebase custom claim via the Auth SDK and return
     * `Result.failure(IllegalAccessException("not admin"))` on
     * rejection.
     *
     * IMPORTANT: until the Firebase project is wired (D9.3), this
     * gate is untrusted. The hardcoded password in source has been
     * removed; in practice any non-blank password will succeed
     * because the stub does not validate. This is intentional during
     * the in-memory phase and is gated on D9.3.
     */
    suspend fun requireAdmin(password: String): Result<Unit>
}
