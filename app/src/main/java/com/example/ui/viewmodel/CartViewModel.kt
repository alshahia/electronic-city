package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.di.ServiceLocator
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.repository.CartRepository
import com.example.data.repository.OutOfStockException
import com.example.ui.locals.LocaleResources
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Cart state + mutations. Exposes derived streams (count / total) the
 * cart screen and the badge in the top bar share.
 */
class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val cartRepo: CartRepository = ServiceLocator.getCartRepository(application.applicationContext)

    val cartItemsDetails: StateFlow<List<Pair<CartItem, Product>>> = cartRepo.cartItemsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartTotal: StateFlow<Double> = cartRepo.totalCartPrice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val cartItemsCount: StateFlow<Int> = cartRepo.cartItemsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addToCart(productId: String) {
        viewModelScope.launch {
            try {
                cartRepo.addToCart(productId)
                MessageBus.publish(LocaleResources.getString(R.string.msg_cart_added))
            } catch (e: OutOfStockException) {
                // H3 / Phase 7B-2 — surface the stock mismatch
                // as a friendly localized message instead of
                // letting the exception bubble. The message is
                // keyed on the localized name, so the user sees
                // "<product> only has N left" (or the generic
                // out-of-stock copy when availableStock == 0).
                val messageRes = if (e.availableStock > 0) {
                    R.string.msg_cart_out_of_stock_with_count
                } else {
                    R.string.msg_cart_out_of_stock
                }
                MessageBus.publish(
                    LocaleResources.getString(messageRes, e.availableStock)
                )
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            cartRepo.updateQuantity(productId, quantity)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            cartRepo.removeFromCart(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepo.clearCart()
        }
    }
}
