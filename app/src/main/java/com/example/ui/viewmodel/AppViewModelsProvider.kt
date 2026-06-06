package com.example.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Composable helper that resolves each of the 7 focused ViewModels against
 * the current [androidx.lifecycle.ViewModelStoreOwner] and bundles them
 * into an [AppViewModels] record. Each VM is its own `viewModel(...)` call
 * keyed by its class so the framework dedupes and scopes them to the
 * owner.
 */
@Composable
fun rememberAppViewModels(
    factory: ViewModelProvider.Factory
): AppViewModels {
    val theme: ThemeViewModel = viewModel(factory = factory)
    val userProfile: UserProfileViewModel = viewModel(factory = factory)
    val shop: ShopViewModel = viewModel(factory = factory)
    val cart: CartViewModel = viewModel(factory = factory)
    val order: OrderViewModel = viewModel(factory = factory)
    val navigation: NavigationViewModel = viewModel(factory = factory)
    val adminAuth: AdminAuthViewModel = viewModel(factory = factory)
    return AppViewModels(
        theme = theme,
        userProfile = userProfile,
        shop = shop,
        cart = cart,
        order = order,
        navigation = navigation,
        adminAuth = adminAuth
    )
}
