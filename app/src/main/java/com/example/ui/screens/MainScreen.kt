package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.locals.LocalAppActivity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.HomeTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vms: AppViewModels,
    transientMessage: String? = null,
    @Suppress("UNUSED_PARAMETER") onTransientMessageShown: () -> Unit = {}
) {
    val selectedTab by vms.navigation.selectedTab.collectAsState()
    val cartItemsCount by vms.cart.cartItemsCount.collectAsState()
    val isOnlineState by vms.shop.isOnline.collectAsState()
    val selectedProductForDetail by vms.shop.selectedProductForDetail.collectAsState()

    var isCartOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = LocalAppActivity.current

    var lastBackPressTime by remember { mutableStateOf(0L) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedProductForDetail != null) {
            vms.shop.showProductDetail(null)
        } else if (isCartOpen) {
            isCartOpen = false
        } else {
            val previous = vms.navigation.popTabHistory()
            if (previous != null) {
                vms.navigation.selectTabDirectly(previous)
            } else if (selectedTab != HomeTab.HOME.index) {
                vms.navigation.selectTabDirectly(HomeTab.HOME.index)
            } else {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_back_exit), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                // Top Custom Header precisely styled like the screenshots
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right: Logo "EC" / User Account Action (Renders on Right under RTL) + Back Arrow (if not on Home screen)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.animateContentSize()
                        ) {
                            if (selectedTab != HomeTab.HOME.index) {
                                IconButton(
                                    onClick = {
                                        val previous = vms.navigation.popTabHistory()
                                        if (previous != null) {
                                            vms.navigation.selectTabDirectly(previous)
                                        } else {
                                            vms.navigation.selectTabDirectly(HomeTab.HOME.index)
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowForward, // Right arrow for back button in RTL Arabic layout
                                        contentDescription = stringResource(R.string.cd_back_to_previous),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { vms.navigation.selectTab(HomeTab.ACCOUNT.index) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.topbar_avatar_text),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Center: "تواصل معنا" phone shortcut button precisely matching screenshots
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:07841703018")
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.topbar_contact),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = stringResource(R.string.cd_topbar_contact),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Left icons: Bell sound + Cart with Count Badge (Renders on Left under RTL)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Bell Icon
                            IconButton(onClick = {}) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = stringResource(R.string.cd_notifications),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Simulated small green notification dot
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            // Shopping Basket Icon
                            IconButton(onClick = { isCartOpen = true }) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = stringResource(R.string.cd_cart),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (cartItemsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(DiscountRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cartItemsCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Network state indicator badge
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOnlineState)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isOnlineState) stringResource(R.string.badge_network_online) else stringResource(R.string.badge_network_offline),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnlineState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom navigation tab positions - reversed to show home on right and account on left.
                // RTL places the FIRST source child on the visual right, so we keep the original
                // source order: HOME first, ACCOUNT last.  This preserves the existing visual layout.
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    // 1. Home / الرئيسية (Shown on Right under RTL, Left under LTR)  → HomeTab.HOME (index 0, D8.20)
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.HOME.index,
                        onClick = { vms.navigation.selectTab(HomeTab.HOME.index) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == HomeTab.HOME.index) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.nav_home)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    // 2. Products / المنتجات
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.PRODUCTS.index,
                        onClick = { vms.navigation.selectTab(HomeTab.PRODUCTS.index) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == HomeTab.PRODUCTS.index) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                contentDescription = stringResource(R.string.nav_products)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_products), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    // 3. Discounts / التخفيضات
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.DISCOUNTS.index,
                        onClick = { vms.navigation.selectTab(HomeTab.DISCOUNTS.index) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == HomeTab.DISCOUNTS.index) Icons.Filled.Percent else Icons.Outlined.Percent,
                                contentDescription = stringResource(R.string.nav_discounts)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_discounts), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    // 4. Favorites / المفضلة
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.FAVORITES.index,
                        onClick = { vms.navigation.selectTab(HomeTab.FAVORITES.index) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == HomeTab.FAVORITES.index) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.nav_favorites)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_favorites), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    // 5. Account / الحساب (Shown on Left under RTL, Right under LTR)  → HomeTab.ACCOUNT (index 4, D8.20)
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.ACCOUNT.index,
                        onClick = { vms.navigation.selectTab(HomeTab.ACCOUNT.index) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == HomeTab.ACCOUNT.index) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = stringResource(R.string.nav_account)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_account), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Resolve the active tab via the enum so adding a new tab is a single edit.
                when (HomeTab.fromIndex(selectedTab)) {
                    HomeTab.HOME -> HomeScreen(
                        vms = vms,
                        onNavigateToCart = { isCartOpen = true }
                    )
                    HomeTab.PRODUCTS -> ProductsScreen(vms = vms)
                    HomeTab.DISCOUNTS -> DiscountsScreen(vms = vms)
                    HomeTab.FAVORITES -> FavoritesScreen(vms = vms)
                    HomeTab.ACCOUNT -> AccountScreen(vms = vms)
                }
            }
        }

        // Animated full overlay Cart Sheet
        AnimatedVisibility(
            visible = isCartOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CartScreen(
                vms = vms,
                onClose = { isCartOpen = false }
            )
        }

        // Animated full overlay Product Details Sheet
        AnimatedVisibility(
            visible = selectedProductForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedProductForDetail?.let { product ->
                ProductDetailScreen(
                    product = product,
                    vms = vms,
                    onClose = { vms.shop.showProductDetail(null) }
                )
            }
        }

        // Floating sync popup notifications (Slide from bottom)
        AnimatedVisibility(
            visible = transientMessage != null,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp) // elevate above bottom bar
                .padding(horizontal = 24.dp)
        ) {
            transientMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = stringResource(R.string.cd_sync),
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Transient messages are owned by the host (MainActivity collects the
        // MessageBus, holds the latest in local state, and clears it after
        // 3.5s).  MainScreen just renders whatever is handed in.
    }
}
