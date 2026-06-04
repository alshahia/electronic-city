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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ECommerceViewModel

@Composable
fun DiscountsScreen(
    viewModel: ECommerceViewModel
) {
    val discountedProducts by viewModel.discountedProductsFlow.collectAsState(initial = emptyList())
    val favoritesSet by viewModel.favoritesSet.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val categories = listOf("الكل", "العناصر الإلكترونية", "لوحات التطوير")

    val filteredDiscounts = discountedProducts.filter { product ->
        selectedCategory == null || selectedCategory == "الكل" || product.categoryAr == selectedCategory
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
                text = "عروض التخفيضات المميزة",
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
                        (categoryName == "الكل" && selectedCategory == null)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (categoryName == "الكل") {
                            viewModel.selectCategory(null)
                        } else {
                            viewModel.selectCategory(categoryName)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "لا توجد عروض تخفيضات نشطة حالياً",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يرجى التحقق لاحقاً لرؤية العروض الجديدة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
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
                        onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                        onCartAdd = { viewModel.addToCart(product.id) },
                        onDoubleClick = { viewModel.showProductDetail(product) }
                    )
                }
            }
        }
    }
}
