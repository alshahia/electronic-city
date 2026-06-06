package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.AppViewModels

/**
 * Root composable that owns the lifecycle "one-shot" effects that used to
 * live in `ECommerceViewModel.init`:
 *
 *  1. Load the saved user profile from `SharedPreferences` once.
 *  2. Trigger the initial product sync from the remote backend.
 *  3. Auto-sync offline orders whenever the connectivity flag flips on.
 *
 * The host ([MainActivity]) just provides an [AppViewModels] bundle and
 * delegates the whole UI tree to [MainScreen].
 */
@Composable
fun AppRoot(
    vms: AppViewModels,
    content: @Composable (AppViewModels) -> Unit = { MainScreen(vms = it) }
) {
    // One-shot bootstrap.
    LaunchedEffect(Unit) {
        vms.userProfile.loadUserProfile()
        vms.shop.triggerSyncProducts()
    }

    // Auto-sync when connectivity flips on.  Mirrors the original
    // ECommerceViewModel behavior — on first composition with an
    // initial `isOnline = true` we will run one sync right away; that's
    // intentional and matches the pre-refactor UX.
    val isOnline by vms.shop.isOnline.collectAsState()
    LaunchedEffect(isOnline) {
        if (isOnline) {
            vms.order.triggerSyncOfflineOrders(vms.profileSnapshot())
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(vms)
        }
    }
}
