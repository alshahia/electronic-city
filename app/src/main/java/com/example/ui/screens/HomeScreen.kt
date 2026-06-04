package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Product
import com.example.ui.theme.*
import com.example.ui.viewmodel.ECommerceViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: ECommerceViewModel,
    onNavigateToCart: () -> Unit
) {
    val featuredProducts by viewModel.featuredProductsFlow.collectAsState(initial = emptyList())
    val discountedProducts by viewModel.discountedProductsFlow.collectAsState(initial = emptyList())
    val allProducts by viewModel.productsFlow.collectAsState(initial = emptyList())
    val favoritesSet by viewModel.favoritesSet.collectAsState()

    val context = LocalContext.current
    val isArabic = true // Fixed store localization in Arabic

    // Categories data from user's visual reference
    val categories = listOf(
        CategoryItem("المعالجات", "Processors", Icons.Filled.Memory),
        CategoryItem("الذاكرة", "Memory", Icons.Filled.Storage),
        CategoryItem("الملحقات", "Peripherals", Icons.Filled.DeveloperBoard),
        CategoryItem("البطاريات ومستلزماتها", "Battery & Accessories", Icons.Filled.BatteryChargingFull),
        CategoryItem("العناصر الإلكترونية", "Components", Icons.Filled.Settings)
    )

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
            title = "الأقسام",
            onActionClick = {
                viewModel.selectCategory(null)
                viewModel.selectTab(3) // switch to Products tab
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
                            viewModel.selectCategory(category.nameAr)
                            viewModel.selectTab(3) // Switch to Products tab
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
                title = "التخفيضات المليئة بالعروض",
                onActionClick = { viewModel.selectTab(2) } // open index 2 (Discounts)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(discountedProducts) { product ->
                    ProductColumnItem(
                        product = product,
                        isFavorite = favoritesSet.contains(product.id),
                        onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                        onCartAdd = { viewModel.addToCart(product.id) },
                        onDoubleClick = { viewModel.showProductDetail(product) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Recently Arrived ("وصل حديثاً")
        SectionHeader(
            title = "وصل حديثاً",
            onActionClick = {
                viewModel.selectCategory(null)
                viewModel.selectTab(3) // switch to direct catalog
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allProducts.take(5)) { product ->
                ProductColumnItem(
                    product = product,
                    isFavorite = favoritesSet.contains(product.id),
                    onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                    onCartAdd = { viewModel.addToCart(product.id) },
                    onDoubleClick = { viewModel.showProductDetail(product) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Featured Products Grid Display ("المنتجات المميزة")
        SectionHeader(
            title = "المنتجات المميزة لك",
            onActionClick = {
                viewModel.selectCategory(null)
                viewModel.selectTab(3)
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(featuredProducts) { product ->
                ProductColumnItem(
                    product = product,
                    isFavorite = favoritesSet.contains(product.id),
                    onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                    onCartAdd = { viewModel.addToCart(product.id) },
                    onDoubleClick = { viewModel.showProductDetail(product) }
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
                    title = "موزع معتمد لبطاريات Tipsun",
                    tagline = "أفضل خلايا ليثيوم وبطاريات راوتر أصلية",
                    accentText = "سعة حقيقية وأداء ممتاز لجميع مشاريعك",
                    backgroundColor = Color(0xFF142410)
                )
                1 -> BannerPage(
                    title = "أجهزة التحكم والقطع الذكية CNC",
                    tagline = "دقة متناهية وأجهزة قياس متقدمة لورش التطوير",
                    accentText = "لوحات حماية ومحركات ذات عزم عالي",
                    backgroundColor = Color(0xFF0F1E24)
                )
                2 -> BannerPage(
                    title = "لوحات الميكروكنترولر وأورانج باي",
                    tagline = "جميع لوحات التحكم وقطع الغيار والتطوير في مكان واحد",
                    accentText = "شاشات قياس وبوكس مبرمجة تعليمية للمهندسين",
                    backgroundColor = Color(0xFF241C10)
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
                    text = "موزع معتمد",
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
            text = "المزيد ×",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onActionClick() }
                .padding(4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductColumnItem(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onCartAdd: () -> Unit,
    onDoubleClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(175.dp)
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
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                // Image loader via Coil falling back to dynamic placeholder graphic
                val isImageError = remember { mutableStateOf(false) }

                if (!isImageError.value) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { isImageError.value = true }
                    )
                }

                // If loading fails or offline, show elegant gradient backdrop with placeholder icon
                if (isImageError.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF81C784), Color(0xFFC8E6C9)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeveloperBoard,
                            contentDescription = "EC Item",
                            tint = ShopGreenDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            text = "EC Shop",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShopGreenDark.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                        )
                    }
                }

                // Favorite Love Icon Toggler
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
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
                            text = "تنزيلات %$percent",
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
                        text = "السعر ${formatPrice(product.originalPrice)} د.ع",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Current Price
                Text(
                    text = "${formatPrice(product.price)} د.ع",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "Add to Basket" Green Button
                Button(
                    onClick = onCartAdd,
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
                            contentDescription = "Add to cart",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "أضف للسلة",
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
