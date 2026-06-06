package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Bundle of the 6 focused ViewModels that survive [MainActivity]'s
 * `viewModelStoreOwner`. Screens request exactly the subset they need.
 */
data class AppViewModels(
    val theme: ThemeViewModel,
    val userProfile: UserProfileViewModel,
    val shop: ShopViewModel,
    val cart: CartViewModel,
    val order: OrderViewModel,
    val navigation: NavigationViewModel,
    val adminAuth: AdminAuthViewModel
) {
    /**
     * Captures a read-only profile snapshot at call time. Used by
     * [OrderViewModel.triggerSyncOfflineOrders] so it doesn't depend on
     * [UserProfileViewModel] directly.
     */
    fun profileSnapshot(): ProfileSnapshot = ProfileSnapshot(
        username = userProfile.username.value,
        phone = userProfile.userPhone.value,
        countryCode = userProfile.userCountryCode.value,
        location = userProfile.userLocation.value,
        avatarIndex = userProfile.userAvatarIndex.value,
        isRegistered = userProfile.isUserRegistered.value
    )
}

/**
 * Single factory for all 6 ViewModels so a `viewModel()` call site only
 * has to mention the bundle. `Theme/UserProfile/Shop/Cart/Order` extend
 * [AndroidViewModel] (they need the `Application`); `Navigation` is a
 * plain [ViewModel] constructed by the default path.
 */
class AppViewModelsFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ThemeViewModel::class.java) ->
                ThemeViewModel(application) as T
            modelClass.isAssignableFrom(UserProfileViewModel::class.java) ->
                UserProfileViewModel(application) as T
            modelClass.isAssignableFrom(ShopViewModel::class.java) ->
                ShopViewModel(application) as T
            modelClass.isAssignableFrom(CartViewModel::class.java) ->
                CartViewModel(application) as T
            modelClass.isAssignableFrom(OrderViewModel::class.java) ->
                OrderViewModel(application) as T
            modelClass.isAssignableFrom(NavigationViewModel::class.java) ->
                NavigationViewModel() as T
            modelClass.isAssignableFrom(AdminAuthViewModel::class.java) ->
                AdminAuthViewModel(application) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
