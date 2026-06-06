package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Product
import com.example.ui.haptics.rememberHapticClick
import com.example.ui.locals.LocalWindowSizeClass
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.ProductsUiState

@Composable
fun HomeScreen(
    vms: AppViewModels,
    onNavigateToCart: () -> Unit
) {
    val uiState by vms.shop.uiState.collectAsState()
    val allProducts = (uiState as? ProductsUiState.Success)?.products.orEmpty()
    val favoritesSet by vms.shop.favoritesSet.collectAsState()

    // Categories data from user's visual reference
    val categories = listOf(
        CategoryItem(stringResource(R.string.category_processors), stringResource(R.string.category_processors), Icons.Filled.Memory),
        CategoryItem(stringResource(R.string.category_memory), stringResource(R.string.category_memory), Icons.Filled.Storage),
        CategoryItem(stringResource(R.string.category_peripherals), stringResource(R.string.category_peripherals), Icons.Filled.DeveloperBoard),
        CategoryItem(stringResource(R.string.category_battery), stringResource(R.string.category_battery), Icons.Filled.BatteryChargingFull),
        CategoryItem(stringResource(R.string.category_components), stringResource(R.string.category_components), Icons.Filled.Settings)
    )

    // Phase 6.1 — render the Error retry card when the catalog has no
    // products and the last sync failed. Loading / Success just keep the
    // normal scroll layout (Loading renders empty carousels, which is
    // the same fallback the previous implementation had).
    if (uiState is ProductsUiState.Error) {
        ErrorRetryCard(
            onRetry = { vms.shop.retrySync() }
        )
        return
    }

    val featuredProducts = allProducts.filter { it.isFeatured }
    val discountedProducts = allProducts.filter { it.isDiscounted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp)
    ) {
        // 1. Promotional Rotating Banner Slider (Simulated with Compose animations!)
        BannerSliderSection()

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Categories Drawer Section ("الاقسام")
        SectionHeader(
            title = stringResource(R.string.section_categories),
            onActionClick = {
                vms.shop.selectCategory(null)
                vms.navigation.selectTab(3) // switch to Products tab
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .width(105.dp)
                        .clickable {
                            vms.shop.selectCategory(category.nameAr)
                            vms.navigation.selectTab(3) // Switch to Products tab
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.nameAr,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = category.nameAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 13.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Discount Offers Carousel ("التخفيضات")
        if (discountedProducts.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.section_discounts_offers),
                onActionClick = { vms.navigation.selectTab(2) } // open index 2 (Discounts)
            )

            ProductsCarousel(
                products = discountedProducts,
                favoritesSet = favoritesSet,
                onFavoriteToggle = { vms.shop.toggleFavorite(it.id) },
                onCartAdd = { vms.cart.addToCart(it.id) },
                onDoubleClick = { vms.shop.showProductDetail(it) },
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Recently Arrived ("وصل حديثاً")
        SectionHeader(
            title = stringResource(R.string.section_recently_arrived),
            onActionClick = {
                vms.shop.selectCategory(null)
                vms.navigation.selectTab(3) // switch to direct catalog
            }
        )

        ProductsCarousel(
            products = featuredProducts,
            favoritesSet = favoritesSet,
            onFavoriteToggle = { vms.shop.toggleFavorite(it.id) },
            onCartAdd = { vms.cart.addToCart(it.id) },
            onDoubleClick = { vms.shop.showProductDetail(it) },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Featured Products Grid Display ("المنتجات المميزة")
        SectionHeader(
            title = stringResource(R.string.section_featured_for_you),
            onActionClick = {
                vms.shop.selectCategory(null)
                vms.navigation.selectTab(3)
            }
        )

        ProductsCarousel(
            products = featuredProducts,
            favoritesSet = favoritesSet,
            onFavoriteToggle = { vms.shop.toggleFavorite(it.id) },
            onCartAdd = { vms.cart.addToCart(it.id) },
            onDoubleClick = { vms.shop.showProductDetail(it) },
        )
    }
}

/**
 * Phase 6.1 — full-bleed retry card shown when ProductsUiState.Error is
 * the only thing the catalog can tell us. Mirrors the same pattern used
 * in ProductsScreen / DiscountsScreen / FavoritesScreen.
 */
@Composable
fun ErrorRetryCard(
    onRetry: () -> Unit
) {
    val haptic = rememberHapticClick()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.error_load_failed),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.error_load_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    haptic()
                    onRetry()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_retry),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BannerSliderSection() {
    var activePage by remember { mutableStateOf(0) }
    val bannerCount = 3

    // Dynamic banner auto-rotation
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            activePage = (activePage + 1) % bannerCount
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(NeutralDark, ShopGreenDark)))
    ) {
        // Dynamic banner content based on activePage
        AnimatedContent(
            targetState = activePage,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "bannerChange"
        ) { page ->
            when (page) {
                0 -> BannerPage(
                    title = stringResource(R.string.banner_1_title),
                    tagline = stringResource(R.string.banner_1_tagline),
                    accentText = stringResource(R.string.banner_1_accent),
                    backgroundColor = BannerDarkOlive
                )
                1 -> BannerPage(
                    title = stringResource(R.string.banner_2_title),
                    tagline = stringResource(R.string.banner_2_tagline),
                    accentText = stringResource(R.string.banner_2_accent),
                    backgroundColor = BannerDarkBlue
                )
                2 -> BannerPage(
                    title = stringResource(R.string.banner_3_title),
                    tagline = stringResource(R.string.banner_3_tagline),
                    accentText = stringResource(R.string.banner_3_accent),
                    backgroundColor = BannerDarkAmber
                )
            }
        }

        // Indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until bannerCount) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activePage == i) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun BannerPage(
    title: String,
    tagline: String,
    accentText: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.banner_authorized_distributor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tagline,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = accentText,
                fontSize = 11.sp,
                color = ShopDarkGreenPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.section_more_link),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onActionClick() }
                .padding(4.dp)
        )
    }
}

/**
 * D9.1 — Renders a HomeScreen product strip. On Compact + Medium
 * widths it stays a horizontal `LazyRow` of cards (175dp on phones,
 * 220dp on tablets in portrait). On Expanded (>= 840dp, large
 * tablets / foldables unfolded / ChromeOS) it swaps to a 2-column
 * `LazyVerticalGrid` so the strip fills the extra horizontal
 * space instead of showing long rows of card-thumbnail combos.
 *
 * The carousel-level layout decision is owned by this helper so
 * each section header above (Discount Offers / Recently Arrived /
 * Featured for You) can stay focused on its copy + action
 * semantics. The width pick for the row branch is computed once
 * per recomposition and reused for every item in the row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductsCarousel(
    products: List<Product>,
    favoritesSet: Set<String>,
    onFavoriteToggle: (Product) -> Unit,
    onCartAdd: (Product) -> Unit,
    onDoubleClick: (Product) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact,
        WindowWidthSizeClass.Medium,
        -> {
            val itemWidth = when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 175.dp
                else -> 220.dp
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products) { product ->
                    ProductColumnItem(
                        product = product,
                        isFavorite = favoritesSet.contains(product.id),
                        onFavoriteToggle = { onFavoriteToggle(product) },
                        onCartAdd = { onCartAdd(product) },
                        onDoubleClick = { onDoubleClick(product) },
                        width = itemWidth,
                    )
                }
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(products) { product ->
                    ProductColumnItem(
                        product = product,
                        isFavorite = favoritesSet.contains(product.id),
                        onFavoriteToggle = { onFavoriteToggle(product) },
                        onCartAdd = { onCartAdd(product) },
                        onDoubleClick = { onDoubleClick(product) },
                        width = null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductColumnItem(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onCartAdd: () -> Unit,
    onDoubleClick: () -> Unit = {},
    // D9.1 — `width = null` lets the parent layout size the card
    // (used inside `LazyVerticalGrid` cells). A non-null value pins
    // the card to that width (used inside horizontal `LazyRow`s).
    // Preserves the original 175dp default so existing call sites
    // and `ProductColumnItem` users elsewhere stay visually
    // identical on Compact phones.
    width: Dp? = 175.dp,
) {
    val haptic = rememberHapticClick()
    val sizeModifier = if (width != null) Modifier.width(width) else Modifier
    Card(
        modifier = sizeModifier
            .padding(vertical = 4.dp)
            .combinedClickable(
                onDoubleClick = onDoubleClick,
                onClick = {}
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product image area with badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Phase 6.8 — Coil's `placeholder` + `error` builders replace
                // the previous isImageError state machine. Renders a flat
                // tile while loading and a tinted error tile on failure.
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_product_placeholder)
                        .error(R.drawable.ic_product_error)
                        .build(),
                    contentDescription = product.nameAr,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Favorite Love Icon Toggler
                IconButton(
                    onClick = {
                        haptic()
                        onFavoriteToggle()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.product_favorite_content_description),
                        tint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Discount Badge Indicator
                if (product.isDiscounted && product.originalPrice != null) {
                    val percent = (((product.originalPrice - product.price) / product.originalPrice) * 100).toInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DiscountRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.product_discount_badge, percent),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Text/Details area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.categoryAr,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = product.nameAr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(28.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Optional Original Price
                if (product.isDiscounted && product.originalPrice != null) {
                    Text(
                        text = stringResource(R.string.product_original_price, formatPrice(product.originalPrice)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Current Price
                Text(
                    text = stringResource(R.string.product_current_price, formatPrice(product.price)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "Add to Basket" Green Button
                Button(
                    onClick = {
                        haptic()
                        onCartAdd()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = stringResource(R.string.product_add_to_cart_content_description),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.product_add_to_cart),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Global category helper class
data class CategoryItem(
    val nameAr: String,
    val nameEn: String,
    val icon: ImageVector
)

// Currency helper
fun formatPrice(price: Double): String {
    return String.format("%,.0f", price)
}
