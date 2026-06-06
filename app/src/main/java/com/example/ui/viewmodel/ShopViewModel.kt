package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.di.ServiceLocator
import com.example.data.model.Product
import com.example.data.repository.FavoriteRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.ProductWriteResult
import com.example.ui.locals.LocaleResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The state the product lists (Home / Products / Discounts / Favorites)
 * can be in. Phase 6.1 surfaces this as a sealed UI state so screens
 * can render an explicit Error retry card instead of a silent empty.
 */
sealed interface ProductsUiState {
    data object Loading : ProductsUiState
    data class Success(val products: List<Product>) : ProductsUiState
    data class Error(val messageRes: Int) : ProductsUiState
}

/**
 * Holds the catalog + product filters + the "open detail" selection.
 *
 * `isOnline` is exposed here (not in [UserProfileViewModel]) because the
 * shop sync is the dominant consumer; profile reads it transitively through
 * the same [ServiceLocator] remote service.
 */
class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val productRepo: ProductRepository = ServiceLocator.getProductRepository(context)
    private val favoriteRepo: FavoriteRepository = ServiceLocator.getFavoriteRepository(context)

    val productsFlow: Flow<List<Product>> = productRepo.allProducts
    val featuredProductsFlow: Flow<List<Product>> = productRepo.featuredProducts
    val discountedProductsFlow: Flow<List<Product>> = productRepo.discountedProducts

    /**
     * H2 / Phase 7B-2 — admin "Archived" tab. Soft-archive only
     * touches the local DB; the row is hidden from the storefront
     * flows but kept around so order history (OrderItem
     * RESTRICT FK) stays intact.
     */
    val archivedProductsFlow: Flow<List<Product>> = productRepo.archivedProducts

    val isOnline: StateFlow<Boolean> = productRepo.isOnline.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true
    )

    val favoritesSet: StateFlow<Set<String>> = favoriteRepo.favorites
        .map { favs -> favs.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Search + category filter state — owned by the shop, not the nav layer,
    // because the search box lives inside the Products tab.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Detail overlay selection.
    private val _selectedProductForDetail = MutableStateFlow<Product?>(null)
    val selectedProductForDetail: StateFlow<Product?> = _selectedProductForDetail.asStateFlow()

    // Refresh state for the home + products screens.
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ─── Phase 6.1: explicit UI state for the product lists ─────────
    // tracks the last sync attempt's result so screens can surface an
    // Error card with a Retry button. When the catalog emits a non-empty
    // list we treat it as Success regardless of the last sync outcome.
    private val _lastSyncOk = MutableStateFlow(true)
    val uiState: StateFlow<ProductsUiState> = combine(
        productRepo.allProducts,
        _isRefreshing,
        _lastSyncOk,
    ) { products, refreshing, lastSyncOk ->
        when {
            refreshing && products.isEmpty() -> ProductsUiState.Loading
            products.isNotEmpty() -> ProductsUiState.Success(products)
            !lastSyncOk -> ProductsUiState.Error(R.string.error_load_failed)
            else -> ProductsUiState.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductsUiState.Loading
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun showProductDetail(product: Product?) {
        _selectedProductForDetail.value = product
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            favoriteRepo.toggleFavorite(productId)
        }
    }

    /**
     * H2 / Phase 7B-2 — soft-archive an admin's product. Publishes
     * a success or failure message via [MessageBus] so the
     * AccountScreen snackbar / `MessageBusCollector` shows it.
     */
    fun archiveProduct(id: String) {
        viewModelScope.launch {
            val result = productRepo.archiveProduct(id)
            val messageRes = if (result.isSuccess) {
                R.string.msg_shop_admin_archived
            } else {
                R.string.msg_shop_admin_archive_failed
            }
            MessageBus.publish(LocaleResources.getString(messageRes))
        }
    }

    /**
     * H2 — restore a soft-archived product. Same MessageBus pattern
     * as [archiveProduct].
     */
    fun unarchiveProduct(id: String) {
        viewModelScope.launch {
            val result = productRepo.unarchiveProduct(id)
            val messageRes = if (result.isSuccess) {
                R.string.msg_shop_admin_unarchived
            } else {
                R.string.msg_shop_admin_archive_failed
            }
            MessageBus.publish(LocaleResources.getString(messageRes))
        }
    }

    fun triggerSyncProducts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = productRepo.syncProducts()
            _isRefreshing.value = false
            _lastSyncOk.value = result.isSuccess
            if (result.isSuccess) {
                MessageBus.publish(LocaleResources.getString(R.string.msg_shop_sync_ok))
            } else {
                MessageBus.publish(LocaleResources.getString(R.string.msg_shop_sync_offline))
            }
        }
    }

    /**
     * Phase 6.1 — explicit retry from the Error card. Same effect as
     * [triggerSyncProducts] but named to read well in onClick handlers.
     */
    fun retrySync() = triggerSyncProducts()

    fun addOrUpdateProduct(
        id: String,
        nameAr: String,
        nameEn: String,
        descriptionAr: String,
        descriptionEn: String,
        imageUrl: String,
        price: Double,
        stock: Int,
        categoryAr: String,
        categoryEn: String,
        isFeatured: Boolean = true,
        isDiscounted: Boolean = false,
        originalPrice: Double? = null,
        onComplete: (ProductWriteResult) -> Unit
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                nameAr = nameAr,
                nameEn = nameEn,
                descriptionAr = descriptionAr,
                descriptionEn = descriptionEn,
                imageUrl = if (imageUrl.isBlank()) {
                    "https://images.unsplash.com/photo-1517420712361-2e6d99c3b6ec?w=400"
                } else imageUrl,
                price = price,
                originalPrice = originalPrice,
                categoryAr = categoryAr,
                categoryEn = categoryEn,
                isFeatured = isFeatured,
                isDiscounted = isDiscounted,
                stock = stock,
                lastUpdated = System.currentTimeMillis()
            )
            val result = productRepo.addOrUpdateProduct(product)
            result.fold(
                onSuccess = { writeResult ->
                    MessageBus.publish(LocaleResources.getString(writeResult.messageRes))
                    onComplete(writeResult)
                },
                onFailure = { error ->
                    MessageBus.publish(LocaleResources.getString(R.string.msg_shop_admin_both_failed))
                    onComplete(ProductWriteResult.BothFailed(error))
                }
            )
        }
    }
}
