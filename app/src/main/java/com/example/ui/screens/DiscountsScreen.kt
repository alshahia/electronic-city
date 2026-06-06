package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.EmptyState
import com.example.ui.locals.LocalWindowSizeClass
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.ProductsUiState

@Composable
fun DiscountsScreen(
    vms: AppViewModels
) {
    val uiState by vms.shop.uiState.collectAsState()
    val favoritesSet by vms.shop.favoritesSet.collectAsState()
    val selectedCategory by vms.shop.selectedCategory.collectAsState()

    // Phase 6.1 — render retry card on Error so the user has a way out
    // of the "no internet + no cached products" dead-end.
    if (uiState is ProductsUiState.Error) {
        ErrorRetryCard(onRetry = { vms.shop.retrySync() })
        return
    }

    val discountedProducts = (uiState as? ProductsUiState.Success)
        ?.products
        ?.filter { it.isDiscounted }
        .orEmpty()

    // Hoist stringResource() out of per-product / per-chip lambdas.
    val allCategory = stringResource(R.string.category_all)
    val componentsCategory = stringResource(R.string.category_components)
    val developmentBoardsCategory = stringResource(R.string.category_development_boards)

    val categories = listOf(
        allCategory,
        componentsCategory,
        developmentBoardsCategory,
    )

    val filteredDiscounts = discountedProducts.filter { product ->
        selectedCategory == null || selectedCategory == allCategory || product.categoryAr == selectedCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Simple title card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.section_discounts_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Categories selector
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(categories) { categoryName ->
                val isSelected = (selectedCategory == categoryName) ||
                        (categoryName == allCategory && selectedCategory == null)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (categoryName == allCategory) {
                            vms.shop.selectCategory(null)
                        } else {
                            vms.shop.selectCategory(categoryName)
                        }
                    },
                    label = {
                        Text(
                            text = categoryName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredDiscounts.isEmpty()) {
            // D9.2 — shared empty-state slot. See ProductsScreen.
            EmptyState(
                illustration = painterResource(R.drawable.empty_discounts_art),
                title = stringResource(R.string.empty_discounts_title),
                subtitle = stringResource(R.string.empty_discounts_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // D9.1 — same width-class bucketing as ProductsScreen /
            // FavoritesScreen. DiscountsScreen is a sibling catalog,
            // so the column count should scale the same way.
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
                items(filteredDiscounts) { product ->
                    ProductColumnItem(
                        product = product,
                        isFavorite = favoritesSet.contains(product.id),
                        onFavoriteToggle = { vms.shop.toggleFavorite(product.id) },
                        onCartAdd = { vms.cart.addToCart(product.id) },
                        onDoubleClick = { vms.shop.showProductDetail(product) }
                    )
                }
            }
        }
    }
}
