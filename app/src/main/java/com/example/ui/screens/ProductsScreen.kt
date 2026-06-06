package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.ShopGreenPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.ProductsUiState

@Composable
fun ProductsScreen(
    vms: AppViewModels
) {
    val uiState by vms.shop.uiState.collectAsState()
    val searchQuery by vms.shop.searchQuery.collectAsState()
    val selectedCategory by vms.shop.selectedCategory.collectAsState()
    val favoritesSet by vms.shop.favoritesSet.collectAsState()

    val allProducts = (uiState as? ProductsUiState.Success)?.products.orEmpty()

    // Hoist stringResource() calls out of per-item lambdas. The filter
    // and items() blocks run once per product per recomposition, so any
    // stringResource() call inside them is wasted work; binding them at
    // composable scope (cheap) and referencing the cached value is cheaper.
    val allCategory = stringResource(R.string.category_all)
    val processorCategory = stringResource(R.string.category_processors)
    val memoryCategory = stringResource(R.string.category_memory)
    val peripheralsCategory = stringResource(R.string.category_peripherals)
    val batteryCategory = stringResource(R.string.category_battery)
    val componentsCategory = stringResource(R.string.category_components)

    // Filter categories matching standard seeds
    val categories = listOf(
        allCategory,
        processorCategory,
        memoryCategory,
        peripheralsCategory,
        batteryCategory,
        componentsCategory,
    )

    // Filter logic
    val filteredProducts = allProducts.filter { product ->
        val matchesSearch = product.nameAr.contains(searchQuery, ignoreCase = true) ||
                product.nameEn.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null ||
                selectedCategory == allCategory ||
                product.categoryAr == selectedCategory

        matchesSearch && matchesCategory
    }

    // Phase 6.1 — render the retry card on Error. Otherwise keep the
    // normal product grid layout with the search box and filter chips.
    if (uiState is ProductsUiState.Error) {
        ErrorRetryCard(onRetry = { vms.shop.retrySync() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Search input Box precisely matching screenshot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vms.shop.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                leadingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vms.shop.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Horizontal categories filter chip bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true // Flow RTL matches your screenshot perfectly!
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
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search statistics or quick tags
        if (searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_results_count, filteredProducts.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { vms.shop.updateSearchQuery("") },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_clear),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Display clean quick search tag suggestions in Arabic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_suggestions_label),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                listOf(
                    stringResource(R.string.search_suggestion_cpu),
                    stringResource(R.string.search_suggestion_memory),
                    stringResource(R.string.search_suggestion_display),
                    stringResource(R.string.search_suggestion_battery),
                ).forEach { tag ->
                    SuggestionChip(
                        onClick = { vms.shop.updateSearchQuery(tag) },
                        label = { Text(text = tag, fontSize = 10.sp) },
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Grid of Products
        if (filteredProducts.isEmpty()) {
            // D9.2 — shared empty-state slot. Replaces an ad-hoc
            // Icon + Spacer + Text + Spacer + Text block that lived
            // in 5 screens with subtle font-size / alpha drift.
            EmptyState(
                illustration = painterResource(R.drawable.empty_search_art),
                title = stringResource(R.string.empty_search_title),
                subtitle = stringResource(R.string.empty_search_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // D9.1 — grid column count adapts to the window width.
            // `Adaptive(180.dp)` lets Compose pick the right count for
            // any width (2 cols on a 360dp phone, 3 on a 600dp tablet,
            // 4-5 on a 840dp+ tablet). No WindowSizeClass branching
            // needed at this layer because the same grid scale-up
            // behavior works at every width.
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
                items(filteredProducts) { product ->
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
