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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ECommerceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ECommerceViewModel
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val cartItemsCount by viewModel.cartItemsCount.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val isOnlineState by viewModel.isOnline.collectAsState()
    val selectedProductForDetail by viewModel.selectedProductForDetail.collectAsState()

    var isCartOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var lastBackPressTime by remember { mutableStateOf(0L) }
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is androidx.activity.ComponentActivity) {
                break
            }
            currentContext = currentContext.baseContext
        }
        currentContext as? androidx.activity.ComponentActivity
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedProductForDetail != null) {
            viewModel.showProductDetail(null)
        } else if (isCartOpen) {
            isCartOpen = false
        } else {
            val previous = viewModel.popTabHistory()
            if (previous != null) {
                viewModel.selectTabDirectly(previous)
            } else if (selectedTab != 4) {
                viewModel.selectTabDirectly(4) // default fallback to Home
            } else {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    android.widget.Toast.makeText(context, "اضغط مرة أخرى للخروج", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Observe offline order syncing auto-trigger on connecting
    LaunchedEffect(isOnlineState) {
        if (isOnlineState) {
            viewModel.triggerSyncOfflineOrders()
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
                            if (selectedTab != 4) {
                                IconButton(
                                    onClick = {
                                        val previous = viewModel.popTabHistory()
                                        if (previous != null) {
                                            viewModel.selectTabDirectly(previous)
                                        } else {
                                            viewModel.selectTabDirectly(4) // Fallback to Home
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowForward, // Right arrow for back button in RTL Arabic layout
                                        contentDescription = "الرجوع للصفحة السابقة",
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
                                    .clickable { viewModel.selectTab(0) }
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
                                        text = "EC",
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
                                text = "تواصل معنا",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Contact us",
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
                                        contentDescription = "Notifications",
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
                                        contentDescription = "Cart",
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
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOnlineState) "متصل" else "دون اتصال",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnlineState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom navigation tab positions - reversed to show home on right and account on left
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    // 1. Main Home / الرئيسية (Shown on Right under RTL)
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.selectTab(4) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "الرئيسية"
                            )
                        },
                        label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        selected = selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                contentDescription = "المنتجات"
                            )
                        },
                        label = { Text("المنتجات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Percent else Icons.Outlined.Percent,
                                contentDescription = "التخفيضات"
                            )
                        },
                        label = { Text("التخفيضات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "الأمنيات"
                            )
                        },
                        label = { Text("الأمنيات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // 5. Account / الحساب (Shown on Left under RTL)
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "الحساب"
                            )
                        },
                        label = { Text("الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                // Switching pages smoothly based on standard index
                when (selectedTab) {
                    4 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToCart = { isCartOpen = true }
                    )
                    3 -> ProductsScreen(viewModel = viewModel)
                    2 -> DiscountsScreen(viewModel = viewModel)
                    1 -> FavoritesScreen(viewModel = viewModel)
                    0 -> AccountScreen(viewModel = viewModel)
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
                viewModel = viewModel,
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
                    viewModel = viewModel,
                    onClose = { viewModel.showProductDetail(null) }
                )
            }
        }

        // Floating sync popup notifications (Slide from bottom)
        AnimatedVisibility(
            visible = syncMessage != null,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp) // elevate above bottom bar
                .padding(horizontal = 24.dp)
        ) {
            syncMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralDark),
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
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = "Sync",
                            tint = ShopDarkGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
