package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.di.ServiceLocator
import com.example.data.model.CartItem
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    object Loading : ProductsUiState
    data class Success(val products: List<Product>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

enum class AppThemeMode {
    System,
    Light,
    Dark
}

class ECommerceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // App Theme State
    private val sharedPrefs = context.getSharedPreferences("ecommerce_prefs", android.content.Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(sharedPrefs.getString("theme_mode", AppThemeMode.System.name) ?: AppThemeMode.System.name)
        } catch (e: Exception) {
            AppThemeMode.System
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode.name).apply()
    }

    // User Profile State
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userCountryCode = MutableStateFlow("+964")
    val userCountryCode: StateFlow<String> = _userCountryCode.asStateFlow()

    private val _userLocation = MutableStateFlow("")
    val userLocation: StateFlow<String> = _userLocation.asStateFlow()

    private val _isUserRegistered = MutableStateFlow(false)
    val isUserRegistered: StateFlow<Boolean> = _isUserRegistered.asStateFlow()

    private val _userAvatarIndex = MutableStateFlow(0)
    val userAvatarIndex: StateFlow<Int> = _userAvatarIndex.asStateFlow()

    fun loadUserProfile() {
        var name = sharedPrefs.getString("user_name", "") ?: ""
        if (name.isBlank()) {
            name = "مستخدم_${(1000..9999).random()}"
            sharedPrefs.edit().putString("user_name", name).apply()
        }
        _username.value = name
        _userPhone.value = sharedPrefs.getString("user_phone", "") ?: ""
        _userCountryCode.value = sharedPrefs.getString("user_country_code", "+964") ?: "+964"
        _userLocation.value = sharedPrefs.getString("user_location", "") ?: ""
        _isUserRegistered.value = sharedPrefs.getBoolean("is_signed_up", false)
        _userAvatarIndex.value = sharedPrefs.getInt("user_avatar_index", 0)
    }

    fun updateUserProfile(name: String, phone: String, countryCode: String, location: String, isRegistered: Boolean, avatarIdx: Int) {
        sharedPrefs.edit().apply {
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_country_code", countryCode)
            putString("user_location", location)
            putBoolean("is_signed_up", isRegistered)
            putInt("user_avatar_index", avatarIdx)
            apply()
        }
        _username.value = name
        _userPhone.value = phone
        _userCountryCode.value = countryCode
        _userLocation.value = location
        _isUserRegistered.value = isRegistered
        _userAvatarIndex.value = avatarIdx

        viewModelScope.launch {
            var isCloudSynced = false
            if (isOnline.value) {
                isCloudSynced = remoteService.uploadUserProfileOnline(
                    username = name,
                    phone = phone,
                    countryCode = countryCode,
                    location = location,
                    avatarIndex = avatarIdx
                )
            }
            if (isCloudSynced) {
                showTemporaryMessage("تمت مزامنة بيانات حسابك السحابي مع خادوم Firebase بنجاح!")
            } else {
                showTemporaryMessage("حُفظ الحساب محلياً وسيتزامن سحابياً بمجرد عودة الاتصال!")
            }
        }
    }

    /**
     * Store Owner uploads a new product or updates an existing one details.
     */
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
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                nameAr = nameAr,
                nameEn = nameEn,
                descriptionAr = descriptionAr,
                descriptionEn = descriptionEn,
                imageUrl = if (imageUrl.isBlank()) "https://images.unsplash.com/photo-1517420712361-2e6d99c3b6ec?w=400" else imageUrl,
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
            if (result.isSuccess) {
                showTemporaryMessage("تم تحديث مستودع المتجر ومزامنته سحابياً مع العملاء!")
                onComplete(true)
            } else {
                showTemporaryMessage("حدث خطأ أثناء الاتصال بالخادوم، حُفظ المنتج محلياً!")
                onComplete(false)
            }
        }
    }

    // Repositories
    private val productRepo = ServiceLocator.getProductRepository(context)
    private val cartRepo = ServiceLocator.getCartRepository(context)
    private val favoriteRepo = ServiceLocator.getFavoriteRepository(context)
    private val orderRepo = ServiceLocator.getOrderRepository(context)
    private val remoteService = ServiceLocator.getRemoteService(context)

    // UI state streams
    val productsFlow: Flow<List<Product>> = productRepo.allProducts
    val featuredProductsFlow: Flow<List<Product>> = productRepo.featuredProducts
    val discountedProductsFlow: Flow<List<Product>> = productRepo.discountedProducts

    // Reactive database streams
    val cartItemsDetails: StateFlow<List<Pair<CartItem, Product>>> = cartRepo.cartItemsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = cartRepo.totalCartPrice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemsCount: StateFlow<Int> = cartRepo.cartItemsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoritesSet: StateFlow<Set<String>> = favoriteRepo.favorites
        .map { favs -> favs.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val ordersList: StateFlow<List<com.example.data.model.OrderWithItems>> = orderRepo.allOrdersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnline: StateFlow<Boolean> = productRepo.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Selected navigation tab index (Default is 4: الرئيسية - Home, to match RTL ordering!)
    // Tabs: 4-الرئيسية (Home), 3-المنتجات (Products), 2-التخفيضات (Discounts), 1-المفضلة (Favorites), 0-الحساب (Account)
    private val _selectedTab = MutableStateFlow(4)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Selected product for full detailed view page
    private val _selectedProductForDetail = MutableStateFlow<Product?>(null)
    val selectedProductForDetail: StateFlow<Product?> = _selectedProductForDetail.asStateFlow()

    fun showProductDetail(product: Product?) {
        _selectedProductForDetail.value = product
    }

    // Interactive Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null) // null = "All / الكل"
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Syncing state indicators
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        loadUserProfile()
        // Initial sync of shop items from virtual Firebase
        triggerSyncProducts()
    }

    private val tabHistory = ArrayList<Int>()

    fun selectTab(index: Int) {
        if (_selectedTab.value != index) {
            tabHistory.add(_selectedTab.value)
            _selectedTab.value = index
        }
    }

    fun selectTabDirectly(index: Int) {
        _selectedTab.value = index
    }

    fun popTabHistory(): Int? {
        if (tabHistory.isNotEmpty()) {
            return tabHistory.removeAt(tabHistory.size - 1)
        }
        return null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Refresh store items from service
    fun triggerSyncProducts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = productRepo.syncProducts()
            _isRefreshing.value = false
            if (result.isSuccess) {
                showTemporaryMessage("تم تحديث المنتجات بنجاح من الخادوم")
            } else {
                showTemporaryMessage("تعذر الاتصال بالإنترنت، تصفح المنتجات المخزنة!")
            }
        }
    }

    // Demo sync: trigger synchronization of cached COD orders when online
    fun triggerSyncOfflineOrders() {
        viewModelScope.launch {
            if (isOnline.value) {
                val syncedCount = orderRepo.syncUnsyncedOrders()
                if (isUserRegistered.value) {
                    remoteService.uploadUserProfileOnline(
                        username = username.value,
                        phone = userPhone.value,
                        countryCode = userCountryCode.value,
                        location = userLocation.value,
                        avatarIndex = userAvatarIndex.value
                    )
                }
                if (syncedCount > 0) {
                    showTemporaryMessage("تم رفع $syncedCount من الطلبات المؤجلة خادومياً ومزامنة حسابك!")
                } else {
                    showTemporaryMessage("جميع الطلبات والبيانات متزامنة بالكامل!")
                }
            } else {
                showTemporaryMessage("لا يزال الهاتف غير متصل بالإنترنت!")
            }
        }
    }

    // Toggle demo online/offline switch
    fun toggleDemoConnectivity() {
        viewModelScope.launch {
            val nextState = !remoteService.isDemoOnlineState()
            remoteService.setDemoOnline(nextState)
            if (nextState) {
                showTemporaryMessage("بث الاتصال بالإنترنت مفعّل (متصل)")
                // Auto-sync offline drafts
                triggerSyncOfflineOrders()
                triggerSyncProducts()
            } else {
                showTemporaryMessage("تم إيقاف الاتصال (وضع عطل الشبكة)")
            }
        }
    }

    // Toast/Snackbar simulation
    private fun showTemporaryMessage(message: String) {
        viewModelScope.launch {
            _syncMessage.value = message
            kotlinx.coroutines.delay(3500)
            if (_syncMessage.value == message) {
                _syncMessage.value = null
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Cart Management
    fun addToCart(productId: String) {
        viewModelScope.launch {
            cartRepo.addToCart(productId)
            showTemporaryMessage("تمت الإضافة إلى السلة بنجاح")
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            cartRepo.updateQuantity(productId, quantity)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            cartRepo.removeFromCart(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepo.clearCart()
        }
    }

    // Favorites Management
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            favoriteRepo.toggleFavorite(productId)
        }
    }

    // Place Cash on Delivery order
    fun submitCODCheckout(
        name: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val total = cartTotal.value
            val currentItems = cartItemsDetails.value

            if (currentItems.isEmpty() || name.isBlank() || phone.isBlank() || address.isBlank()) {
                showTemporaryMessage("يرجى ملء جميع الحقول المطلوبة لإرسال الطلب")
                return@launch
            }

            // Build OrderItem list (proper normalized schema — no JSON round-trip)
            val orderItems: List<OrderItem> = currentItems.map { (item, product) ->
                OrderItem(
                    orderId = "",
                    productId = product.id,
                    nameAr = product.nameAr,
                    nameEn = product.nameEn,
                    price = product.price,
                    quantity = item.quantity
                )
            }

            val result = orderRepo.placeCODOrder(
                customerName = name,
                customerAddress = address,
                customerPhone = phone,
                totalPrice = total,
                items = orderItems
            )

            if (result.isSuccess) {
                val order = result.getOrNull()
                if (order?.isSynced == true) {
                    showTemporaryMessage("تم تقديم طلبك بنجاح ومزامنته مع الخادوم!")
                } else {
                    showTemporaryMessage("تم حفظ الطلب محلياً! سيتم مزامنته تلقائياً عند عودة الإنترنت.")
                }
                onSuccess()
            } else {
                showTemporaryMessage("تعذر معالجة الطلب في الوقت الحالي")
            }
        }
    }

    // Online customers synchronized flow (Admin panel auxiliary view)
    private val _onlineCustomers = MutableStateFlow<List<UserProfile>>(emptyList())
    val onlineCustomers: StateFlow<List<UserProfile>> = _onlineCustomers.asStateFlow()

    fun loadOnlineCustomers() {
        viewModelScope.launch {
            try {
                if (isOnline.value) {
                    val customers = remoteService.getCustomersOnline()
                    _onlineCustomers.value = customers
                }
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }
}
