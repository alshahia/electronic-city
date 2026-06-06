package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.di.ServiceLocator
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.remote.NetworkSimulation
import com.example.data.remote.RemoteDatabaseService
import com.example.data.repository.OrderRepository
import com.example.data.repository.OutOfStockException
import com.example.ui.locals.LocaleResources
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the user's order list, the COD checkout submit, and the offline →
 * online sync trigger. Also exposes the "toggle demo connectivity" admin
 * action because it's the only entry point that affects order sync.
 */
class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val orderRepo: OrderRepository = ServiceLocator.getOrderRepository(context)
    // D9.3 — typed as the contract; the impl is picked by
    // `ServiceLocator` based on `BuildConfig.USE_FIREBASE`.
    private val remoteService: RemoteDatabaseService = ServiceLocator.getRemoteService(context)
    // D9.3 — optional demo-only connectivity toggle. `null` when the
    // active backend is real Firestore (release builds), in which
    // case the admin toggle becomes a no-op.
    private val networkSimulation: NetworkSimulation? = ServiceLocator.getNetworkSimulation(context)

    val ordersList: StateFlow<List<OrderWithItems>> = orderRepo.allOrdersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun submitCODCheckout(
        name: String,
        phone: String,
        address: String,
        cartTotal: Double,
        cartItems: List<Pair<com.example.data.model.CartItem, com.example.data.model.Product>>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (cartItems.isEmpty() || name.isBlank() || phone.isBlank() || address.isBlank()) {
                MessageBus.publish(LocaleResources.getString(R.string.msg_order_missing_fields))
                return@launch
            }

            val orderItems: List<OrderItem> = cartItems.map { (item, product) ->
                OrderItem(
                    orderId = "",
                    productId = product.id,
                    nameAr = product.nameAr,
                    nameEn = product.nameEn,
                    price = product.price,
                    quantity = item.quantity
                )
            }

            val result = orderRepo.placeCODOrder(
                customerName = name,
                customerAddress = address,
                customerPhone = phone,
                totalPrice = cartTotal,
                items = orderItems
            )

            if (result.isSuccess) {
                val order = result.getOrNull()
                if (order?.isSynced == true) {
                    MessageBus.publish(LocaleResources.getString(R.string.msg_order_synced))
                } else {
                    MessageBus.publish(LocaleResources.getString(R.string.msg_order_local))
                }
                onSuccess()
            } else {
                // H3 / Phase 7B-2 — distinguish stock-mismatch
                // failures from generic "could not process
                // order" failures. The user gets actionable
                // copy and the cart stays intact so they can
                // adjust. We use the order-specific string
                // (`msg_order_out_of_stock`) rather than the
                // cart copy because the mental model is
                // different at this point — the user clicked
                // "Place order" and we owe them an order-level
                // error message, not "added to cart" copy.
                val failure = result.exceptionOrNull()
                if (failure is OutOfStockException) {
                    MessageBus.publish(
                        LocaleResources.getString(R.string.msg_order_out_of_stock)
                    )
                } else {
                    MessageBus.publish(LocaleResources.getString(R.string.msg_order_failed))
                }
            }
        }
    }

    fun triggerSyncOfflineOrders(profileSnapshot: ProfileSnapshot?) {
        viewModelScope.launch {
            if (remoteService.checkConnectionDirect()) {
                val syncedCount = orderRepo.syncUnsyncedOrders()
                if (profileSnapshot?.isRegistered == true) {
                    remoteService.uploadUserProfileOnline(
                        username = profileSnapshot.username,
                        phone = profileSnapshot.phone,
                        countryCode = profileSnapshot.countryCode,
                        location = profileSnapshot.location,
                        avatarIndex = profileSnapshot.avatarIndex
                    )
                }
                if (syncedCount > 0) {
                    MessageBus.publish(
                        LocaleResources.getString(R.string.msg_orders_resynced, syncedCount)
                    )
                } else {
                    MessageBus.publish(LocaleResources.getString(R.string.msg_orders_already_synced))
                }
            } else {
                MessageBus.publish(LocaleResources.getString(R.string.msg_still_offline))
            }
        }
    }

    fun toggleDemoConnectivity() {
        viewModelScope.launch {
            // D9.3 — if the active backend is real Firestore, the demo
            // toggle is unavailable. Publish a one-off message and
            // bail. The "demo offline" UX is only meaningful when
            // there's an in-memory backend whose [isOnlineFlow] honors
            // the flag.
            val sim = networkSimulation ?: run {
                MessageBus.publish(LocaleResources.getString(R.string.msg_demo_unavailable))
                return@launch
            }
            val nextState = !sim.isOnline()
            sim.setOnline(nextState)
            if (nextState) {
                MessageBus.publish(LocaleResources.getString(R.string.msg_demo_online))
            } else {
                MessageBus.publish(LocaleResources.getString(R.string.msg_demo_offline))
            }
        }
    }
}

/**
 * Read-only snapshot of the current user profile, captured at the call
 * site so [OrderViewModel] doesn't have to depend on [UserProfileViewModel]
 * directly.
 */
data class ProfileSnapshot(
    val username: String,
    val phone: String,
    val countryCode: String,
    val location: String,
    val avatarIndex: Int,
    val isRegistered: Boolean
)
