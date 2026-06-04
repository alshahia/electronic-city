# إلكترونك-سيتي — Active Refactor Plan

This is the **single source of truth** for the active refactor work.
Closed items are checked off; future backlog is in §"Future i18n Backlog".

For architectural decisions (Room vs SQLDelight, font choice, ViewModel
scoping, etc.) see [`DECISIONS.md`](./DECISIONS.md).

---

## 📋 Phase 0 — Pre-flight ✅

- [x] Create `DECISIONS.md` locking 7 architectural choices
- [x] Move `Verification & Validation System Prompt.md` → `.context/`
- [x] Move `metadata.json` → `docs/legacy-ai-studio/`

---

## 🛠️ Phase 1 — Persistence Layer

### 1.1 Replace `Order.itemsJson: String` with proper entities

- [ ] Add `OrderItem` entity (id, orderId, productId, nameAr, price, quantity)
- [ ] Add `OrderItemDao` (insert / getForOrder / deleteForOrder)
- [ ] Add `OrderWithItems` `@Relation` data class
- [ ] Bump `AppDatabase` version 1 → 2 with `fallbackToDestructiveMigration()`
- [ ] Remove `itemsJson: String` from `Order`
- [ ] `OrderRepository.placeCODOrder` becomes `@Transaction` over orders + order_items
- [ ] `OrderDao.getAllOrdersFlow()` returns `Flow<List<OrderWithItems>>`
- [ ] Consumers (ViewModel) drop `JSONArray`/`JSONObject` imports

### 1.2 Optimize `CartRepository.addToCart`

- [ ] Add `CartDao.getQuantity(productId): Int?` (O(1) lookup)
- [ ] Rewrite `CartRepository.addToCart` to use `getQuantity`

---

## 🧠 Phase 2 — ViewModel Split (the big one)

Split the 426-line `ECommerceViewModel` into 6 focused, Activity-scoped VMs.

### 2.1 Create focused ViewModels

- [ ] `ThemeViewModel` — `themeMode`, `setThemeMode`
- [ ] `UserProfileViewModel` — username, phone, country, location, isRegistered, avatarIndex, onlineCustomers
- [ ] `ShopViewModel` — productsFlow, featuredProductsFlow, discountedProductsFlow, searchQuery, selectedCategory, selectedProductForDetail, productsUiState (with `Error` branch activated), isRefreshing
- [ ] `CartViewModel` — cartItemsDetails, cartTotal, cartItemsCount, addToCart, updateQuantity, removeFromCart, clearCart
- [ ] `OrderViewModel` — ordersList, submitCODCheckout, triggerSyncOfflineOrders
- [ ] `NavigationViewModel` — selectedTab, tabHistory, selectTab, selectTabDirectly, popTabHistory

### 2.2 Eliminate `init` side-effects

- [ ] Move `loadUserProfile()` from `init` to `LaunchedEffect(Unit)` in a new `AppRoot` composable
- [ ] Move `triggerSyncProducts()` from `init` to same `LaunchedEffect`

### 2.3 Constants

- [ ] `UserProfile.DEFAULT_COUNTRY_CODE = "+964"` in companion object

### 2.4 Tab enum (no behavior change yet)

- [ ] Create `enum class HomeTab(val index: Int) { HOME(4), PRODUCTS(3), DISCOUNTS(2), FAVORITES(1), ACCOUNT(0) }`
- [ ] Replace magic numbers in `MainScreen.kt:373` and `MainScreen.kt:267-363`
- [ ] Document that forward numbering + `LocalLayoutDirection` is the future i18n target

### 2.5 Migrate screens

- [ ] Create `AppViewModels` holder data class
- [ ] `MainScreen` takes `AppViewModels`; each tab screen takes only the VMs it needs
- [ ] Delete `ECommerceViewModel.kt`

---

## 🎨 Phase 3 — Theme & Visual Polish

### 3.1 `MyApplicationTheme` perf

- [ ] Wrap `colorScheme` and `Typography` in `remember(darkTheme) { ... }`

### 3.2 Replace hardcoded colors

- [ ] `MainScreen.kt:428` — `containerColor = NeutralDark` → `inverseSurface`
- [ ] CI grep: `rg "Color\(0x" app/src/main/java/com/example/ui/screens/` → must return 0

### 3.3 Google Fonts with layered fallback (D3)

- [ ] Add `androidx.compose.ui:ui-text-google-fonts` to `libs.versions.toml` + `app/build.gradle.kts`
- [ ] Create `ui/theme/FontFamilies.kt` with `DisplayFont`, `BodyFont` (Google Fonts primary, system fallback)
- [ ] Create `res/values/font_certs.xml` with `com_google_android_gms_fonts_certs`
- [ ] Override `displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small` in `Typography`
- [ ] Add OFL attribution to `strings.xml`

### 3.4 Card depth in light mode

- [ ] `Theme.kt:38` — `surfaceVariant = Color(0xFFEEF3E6)` (subtle differentiation from background)

### 3.5 Network badge label

- [ ] `MainScreen.kt:249` — `fontSize = 8.sp` → `11.sp` + padding rebalance

---

## 🔄 Phase 4 — Activity / Lifecycle

### 4.1 Replace context-walking

- [ ] Create `LocalAppActivity` composition local in `MainActivity.kt`
- [ ] `MainScreen.kt:42-52` uses `LocalAppActivity.current` instead of the `while` loop

### 4.2 Edge-to-edge audit

- [ ] Add `Modifier.imePadding()` to checkout form in `CartScreen.kt`

---

## 🛡️ Phase 5 — Build & Release Hardening

### 5.1 R8 / ProGuard

- [ ] `release`: `isMinifyEnabled = true`, `isShrinkResources = true`
- [ ] `proguard-rules.pro`: keep rules for Room entities/DAOs, Moshi @JsonClass, Compose
- [ ] `./gradlew assembleRelease` succeeds
- [ ] Capture pre/post APK sizes

### 5.2 Keystore

- [ ] Fail loud if `KEYSTORE_PATH` env var is missing

### 5.3 detekt + lint (D4)

- [ ] Add detekt plugin (1.23.x) to version catalog + root + app build files
- [ ] Create `config/detekt.yml` (build upon default, disable noisy rules)
- [ ] `lint { warningsAsErrors = true }` for release
- [ ] Add baseline file

---

## ✨ Phase 6 — UX Polish (in-scope only, D5)

- [ ] **6.1** Show `ProductsUiState.Error` as a retry card in `HomeScreen` / `ProductsScreen`
- [ ] **6.4** Haptic feedback on `addToCart` and `toggleFavorite`
- [ ] **6.5** Extract hardcoded strings to `values/strings.xml` + `values-ar/strings.xml`
- [ ] **6.8** Coil `placeholder` + `error` drawables in `AsyncImage` calls

---

## ✅ Phase 7 — Final Verification

- [ ] `./gradlew clean assembleDebug` passes
- [ ] `./gradlew assembleRelease` passes
- [ ] `./gradlew test connectedDebugAndroidTest` all green
- [ ] `./gradlew lint` no errors
- [ ] `./gradlew detekt` no errors
- [ ] `./gradlew :app:dependencies` no `+` versions
- [ ] Manual smoke on emulator (all tabs, theme toggle, test order)
- [ ] Update `README.md` with new commands

---

## 🌐 Future i18n Backlog (deferred — D2)

When Arabic + English LTR/RTL support is on the roadmap:

- [ ] Add `AppCompatDelegate.setApplicationLocales(...)` in `MainActivity`
- [ ] Add `values-en/strings.xml` (mirror of `values-ar/strings.xml`)
- [ ] Switch `HomeTab` to forward numbering (Home=0, Account=4)
- [ ] Drop `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` in favor of locale-driven direction
- [ ] Add `Locale`-aware string resolution: `LocalContext.current.getString(R.string.xxx)`
- [ ] Verify all hardcoded `LayoutDirection.Rtl` references in screens
- [ ] Add `WindowSizeClass` adaptive layouts for tablets
- [ ] Add empty-state illustrations (designer asset)
- [ ] Wire real Firebase Auth + Firestore (replace `FirebaseDatabaseServiceImpl` in-memory mock)
- [ ] FCM push notifications
- [ ] Bundle `.ttf` font assets as a 3rd-tier offline fallback (after Google Fonts + system)
