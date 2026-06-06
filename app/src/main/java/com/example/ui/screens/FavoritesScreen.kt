package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.EmptyState
import com.example.ui.locals.LocalWindowSizeClass
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.ProductsUiState

@Composable
fun FavoritesScreen(
    vms: AppViewModels
) {
    val uiState by vms.shop.uiState.collectAsState()
    val favoritesSet by vms.shop.favoritesSet.collectAsState()

    if (uiState is ProductsUiState.Error) {
        ErrorRetryCard(onRetry = { vms.shop.retrySync() })
        return
    }

    val allProducts = (uiState as? ProductsUiState.Success)?.products.orEmpty()
    val favoriteProducts = allProducts.filter { favoritesSet.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Headers matching Arabic layout order
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Offline persistence confirmation badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.section_wishlist_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.section_wishlist_offline_badge),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = stringResource(R.string.section_wishlist_count, favoriteProducts.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (favoriteProducts.isEmpty()) {
            // D9.2 — shared empty-state slot. The previous
            // implementation used an `Icons.Filled.Favorite`
            // material icon at 64dp; the new vector drawable
            // ships at 120dp for a stronger visual weight.
            EmptyState(
                illustration = painterResource(R.drawable.empty_wishlist_art),
                title = stringResource(R.string.empty_wishlist_title),
                subtitle = stringResource(R.string.empty_wishlist_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // D9.1 — grid column count adapts to the window width.
            // See ProductsScreen for the same pattern. FavoritesScreen
            // reads the size class straight from the composition local
            // so the code path is uniform across both catalog-style
            // grids.
            val windowSizeClass = LocalWindowSizeClass.current
            val gridMinSize = when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 180.dp
                WindowWidthSizeClass.Medium -> 220.dp
                else -> 260.dp
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = gridMinSize),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(favoriteProducts) { product ->
                    ProductColumnItem(
                        product = product,
                        isFavorite = true,
                        onFavoriteToggle = { vms.shop.toggleFavorite(product.id) },
                        onCartAdd = { vms.cart.addToCart(product.id) },
                        onDoubleClick = { vms.shop.showProductDetail(product) }
                    )
                }
            }
        }
    }
}
