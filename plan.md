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

- [x] Add `OrderItem` entity (id, orderId, productId, nameAr, price, quantity)
- [x] Add `OrderItemDao` (insert / getForOrder / deleteForOrder)
- [x] Add `OrderWithItems` `@Relation` data class
- [x] Bump `AppDatabase` version 1 → 2 with `fallbackToDestructiveMigration()`
- [x] Remove `itemsJson: String` from `Order`
- [x] `OrderRepository.placeCODOrder` becomes `@Transaction` over orders + order_items
- [x] `OrderDao.getAllOrdersFlow()` returns `Flow<List<OrderWithItems>>`
- [x] Consumers (ViewModel) drop `JSONArray`/`JSONObject` imports

### 1.2 Optimize `CartRepository.addToCart`

- [x] Add `CartDao.getQuantity(productId): Int?` (O(1) lookup)
- [x] Rewrite `CartRepository.addToCart` to use `getQuantity`

---

## 🧠 Phase 2 — ViewModel Split (the big one)

Split the 426-line `ECommerceViewModel` into 6 focused, Activity-scoped VMs.

### 2.1 Create focused ViewModels

- [x] `ThemeViewModel` — `themeMode`, `setThemeMode`
- [x] `UserProfileViewModel` — username, phone, country, location, isRegistered, avatarIndex, onlineCustomers
- [x] `ShopViewModel` — productsFlow, featuredProductsFlow, discountedProductsFlow, searchQuery, selectedCategory, selectedProductForDetail, isRefreshing
- [x] `CartViewModel` — cartItemsDetails, cartTotal, cartItemsCount, addToCart, updateQuantity, removeFromCart, clearCart
- [x] `OrderViewModel` — ordersList, submitCODCheckout, triggerSyncOfflineOrders
- [x] `NavigationViewModel` — selectedTab, tabHistory, selectTab, selectTabDirectly, popTabHistory

### 2.2 Eliminate `init` side-effects

- [x] Move `loadUserProfile()` from `init` to `LaunchedEffect(Unit)` in a new `AppRoot` composable
- [x] Move `triggerSyncProducts()` from `init` to same `LaunchedEffect`

### 2.3 Constants

- [x] `UserProfile.DEFAULT_COUNTRY_CODE = "+964"` in companion object

### 2.4 Tab enum (no behavior change yet)

- [x] Create `enum class HomeTab(val index: Int) { HOME(4), PRODUCTS(3), DISCOUNTS(2), FAVORITES(1), ACCOUNT(0) }`
- [x] Replace magic numbers in `MainScreen.kt` (BottomBar + `when` dispatch)
- [x] Document that forward numbering + `LocalLayoutDirection` is the future i18n target

### 2.5 Migrate screens

- [x] Create `AppViewModels` holder data class + `AppViewModelsFactory`
- [x] `MainScreen` takes `AppViewModels`; each tab screen takes only the VMs it needs
- [x] Delete `ECommerceViewModel.kt`

### 2.6 Snackbar migration

- [x] Replace VM-owned `syncMessage` StateFlow with process-wide `MessageBus` (SharedFlow)
- [x] `MainActivity` collects the bus, drives a 3.5s local `syncMessage` for `MainScreen` to render

---

## 🎨 Phase 3 — Theme & Visual Polish

### 3.1 `MyApplicationTheme` perf

- [x] Wrap `colorScheme` and `Typography` in `remember(darkTheme) { ... }`

### 3.2 Replace hardcoded colors

- [x] `MainScreen.kt:428` — `containerColor = NeutralDark` → `inverseSurface`
- [x] CI grep: `rg "Color\(0x" app/src/main/java/com/example/ui/screens/` → must return 0

### 3.3 Google Fonts with layered fallback (D3)

- [x] Add `androidx.compose.ui:ui-text-google-fonts` to `libs.versions.toml` + `app/build.gradle.kts`
- [x] Create `ui/theme/FontFamilies.kt` with `DisplayFont`, `BodyFont` (Google Fonts primary, system fallback)
- [x] Create `res/values/font_certs.xml` with `com_google_android_gms_fonts_certs`
- [x] Override `displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small` in `Typography`
- [x] Add OFL attribution to `strings.xml`

### 3.4 Card depth in light mode

- [x] `Theme.kt:38` — `surfaceVariant = Color(0xFFEEF3E6)` (subtle differentiation from background)

### 3.5 Network badge label

- [x] `MainScreen.kt:249` — `fontSize = 8.sp` → `11.sp` + padding rebalance

---

## 🔄 Phase 4 — Activity / Lifecycle

### 4.1 Replace context-walking

- [x] Create `LocalAppActivity` composition local in `MainActivity.kt`
- [x] `MainScreen.kt:42-52` uses `LocalAppActivity.current` instead of the `while` loop

### 4.2 Edge-to-edge audit

- [x] Add `Modifier.imePadding()` to checkout form in `CartScreen.kt`

---

## 🛡️ Phase 5 — Build & Release Hardening

### 5.1 R8 / ProGuard

- [x] `release`: `isMinifyEnabled = true`, `isShrinkResources = true`
- [x] `proguard-rules.pro`: keep rules for Room entities/DAOs, Moshi @JsonClass, Compose
- [ ] `./gradlew assembleRelease` succeeds (blocked — no `gradlew` / Java on PATH)
- [ ] Capture pre/post APK sizes (blocked — needs 5.1 first)

### 5.2 Keystore

- [x] Fail loud if `KEYSTORE_PATH` env var is missing
- [x] Same fail-loud for `STORE_PASSWORD` and `KEY_PASSWORD`

### 5.3 detekt + lint (D4)

- [x] Add detekt plugin (1.23.x) to version catalog + root + app build files
- [x] Create `config/detekt/detekt.yml` (build upon default, disable noisy rules)
- [x] `lint { warningsAsErrors = true; abortOnError = true; checkReleaseBuilds = true }` for release
- [x] Add baseline files (`config/detekt/detekt-baseline.xml`, `app/lint-baseline.xml`)
- [x] Add `detektPlugins` for `detekt-formatting:1.23.7`

---

## ✨ Phase 6 — UX Polish (in-scope only, D5)

### 6.1 Error retry card
- [x] `ShopViewModel.uiState: StateFlow<ProductsUiState>` (Loading/Success/Error) derived from `allProducts × isRefreshing × lastSyncOk`
- [x] `ShopViewModel.retrySync()` re-triggers the product sync
- [x] `ErrorRetryCard` composable in `HomeScreen.kt` (CloudOff icon + `error_load_failed` + `error_load_subtitle` + `action_retry` button, haptics on click)
- [x] `HomeScreen`, `ProductsScreen`, `DiscountsScreen`, `FavoritesScreen` early-return on `ProductsUiState.Error` to render the retry card

### 6.4 Haptics
- [x] `ui/haptics/Haptics.kt` — `rememberHapticFeedback()` + `rememberHapticClick()` (LongPress type; stable via `remember(haptic)`)
- [x] Haptics wired on `ProductColumnItem` `onFavoriteToggle` + `onCartAdd`
- [x] Haptics wired on `ProductDetailScreen` `addToCart` button + favorite `IconButton`
- [x] Haptics wired on `ErrorRetryCard` retry button

### 6.5 String extraction
- [x] `values/strings.xml` — 50+ new keys (section headers, search, empty states, error, product card, banners, categories, all 16 MessageBus messages)
- [x] `values-ar/strings.xml` — explicit Arabic mirror (per D2 future i18n plan; future `values-en/` slot preserved)
- [x] `CartViewModel`, `OrderViewModel`, `ShopViewModel`, `UserProfileViewModel` use `getApplication<Application>().getString(R.string.xxx)` for `MessageBus.publish`
- [x] `HomeScreen`, `ProductsScreen`, `DiscountsScreen`, `FavoritesScreen` use `stringResource` for section headers, empty states, search, banners, product card
- [x] `OrderViewModel` uses positional `getString(R.string.msg_orders_resynced, syncedCount)` for the dynamic count

### 6.5b String extraction extension (all screen files)
- [x] `values/strings.xml` & `values-ar/strings.xml` extended to ~200 keys (namespacing: `cd_*`, `nav_*`, `topbar_*`, `badge_*`, `cart_*`, `detail_*`, `account_*`, `account_option_*`, `account_sheet_*`, `profile_*`, `admin_auth_*`, `admin_tab_*`, `admin_product_*`, `admin_inventory_*`, `admin_customers_*`, `order_*`, `contact_*`, `privacy_*`, `terms_*`, `logout_*`, `currency_iqd`, `price_with_currency`)
- [x] `MainScreen.kt` extracted (16 strings: nav labels, top bar, back arrow CD, sync CD, network badge)
- [x] `ProductDetailScreen.kt` extracted (11 strings: add-to-cart, price/currency format, stock label, discount overlay, specs, ref id, offline notice, EC logo CD, save wishlist CD)
- [x] `CartScreen.kt` extracted (17 strings: title, back CD, CostRow signature updated to take `currency` param, checkout form, empty state, success dialog online/offline variants, item CDs)
- [x] `AccountScreen.kt` extracted (chrome, profile form, admin auth, admin product form, admin inventory, admin customers, orders, info dialog bodies, OrderItemCard sync badge, status steps, total) — remaining Arabic literals are sentinel values (default state, GPS template, category chip match keys, status mapping from server data), not UI strings

### 6.8 Coil placeholders
- [x] `res/drawable/ic_product_placeholder.xml` — flat brand-tinted rounded tile (`#EEF3E6`)
- [x] `res/drawable/ic_product_error.xml` — amber warning tile with a centered broken-image icon
- [x] HomeScreen `ProductColumnItem`, CartScreen, ProductDetailScreen all use Coil's `placeholder()` + `error()` builders

---

## ✅ Phase 7 — Final Verification

Per D8.17, Phase 7 is split into two sub-phases so we can ship the
code-review/docs items now and gate the build items behind the missing
`gradlew`/Java toolchain (user is providing it later).

### 7A — Build verification (blocked until env ready)
- [ ] `./gradlew clean assembleDebug` passes
- [ ] `./gradlew assembleRelease` passes
- [ ] `./gradlew test connectedDebugAndroidTest` all green
- [ ] `./gradlew lint` no errors
- [ ] `./gradlew detekt` no errors
- [ ] `./gradlew :app:dependencies` no `+` versions
- [ ] Manual smoke on emulator (all tabs, theme toggle, test order)
- [ ] Capture pre/post APK sizes (R8 minify + resource shrink delta)

### 7B — Code review & docs (doable now)
- [x] Update `README.md` with R8 / detekt / lint / fail-loud keystore commands
- [x] Update `RUN_GUIDE.md` to mention the same
- [x] Manual review of `proguard-rules.pro` against actual Room/Moshi/Compose/Kotlin metadata use
  - Removed dead `com.example.data.remote.dto.**` keep (no such package exists)
  - `data.model.**` keep is correct (covers `Product.kt` and any future models)
  - `@JsonClass` keep is defensive — no current usages, but Moshi is in dependencies
- [x] Manual review of `config/detekt/detekt.yml` rule thresholds vs codebase (LongMethod, TooManyFunctions, MagicNumber allowlist)
  - MagicNumber allowlist is fine: only "magic" numbers in the codebase are property declarations (delivery fee = 5000.0) and named arguments (seed product prices) — both already ignored by detekt config (`ignorePropertyDeclaration`, `ignoreNamedArgument`)
- [x] Dead-code sweep (incl. pre-existing `CartScreen.kt:63-67` `savedLocation.substringAfter("بغداد")` dead code, redundant `isImageError` Compose fallback in CartScreen/ProductDetailScreen)
  - Fixed `CartScreen.kt:63-67` dead code: now strips the `"GPS:"` prefix from delivery addresses fetched by GPS (was a no-op branch — `savedLocation.substringAfter("بغداد")` was computed and discarded)
  - `isImageError` Compose fallback in CartScreen/ProductDetailScreen is **intentional**, per D8.15 — Coil `.error()` is a brief flash, the Compose fallback handles long failures. Not dead code.
- [x] Confirm `cd_*` content-description key coverage for all `Icon(..., contentDescription = ...)` sites
  - Found and fixed 2 hardcoded English CDs in `ProductsScreen.kt` (lines 107, 114): `"Clear"` → `R.string.cd_clear`, `"Search"` → `R.string.cd_search` (keys already existed)
  - All other contentDescription sites use `stringResource(R.string.cd_*)`, `null` (decorative), `product.nameAr`/`category.nameAr` (data-driven), or a passed-in `title` param

### 7B-2 — Admin Panel Audit Remediation ✅ (shipped 2026-06-05)

> **Source**: multi-dimensional audit of `AccountScreen.kt` lines 789–2138
> (`AccountSheet.AdminPanel`), driven by the prompt at
> `.context/verification-validation-system-prompt.md`. Surfaced 23 issues
> (2 Critical / 6 High / 5 Medium / 8 Low) across security, logic,
> architecture, UX, performance, and maintainability. Sign-off received
> for the 6 pre-flight decisions; execution proceeds in dependency order.

**Pre-flight decisions (locked)**

| # | Decision | Choice | Implication |
|---|---|---|---|
| C1 | Customer type | **`typealias Customer = UserProfile`** | One-line fix in `data/model/Customer.kt`. Zero call-site change. |
| H2 | Delete policy | **Soft-archive** via `archivedAt: Long?` | New column + Room v2→v3 migration + "Archived" admin tab. Preserves `OrderItem` history (FK is RESTRICT). |
| H3 | Stock guard | **Both** — check at add-to-cart AND at order placement | `CartRepository.addToCart` throws `OutOfStockException`; `OrderRepository.placeCODOrder` re-verifies in `withTransaction`; `ProductDetailScreen` disables Add-to-Cart at 0. |
| M3 | Discount fields | **Wire** them (not drop) | `Product.isDiscounted` and `Product.originalPrice` columns already exist; only the admin upload call site fails to pass them. One-line wire-through + admin form re-validation + display. |
| L7 | Admin form state | **Refactor** 13 vars → `data class AdminProductDraft` | Cleaner reset; one `remember` slot. |
| T4.5 | Migration | **Real `Migration(2, 3)` + Robolectric test** | `ALTER TABLE products ADD COLUMN archivedAt INTEGER`. Test boots v2 with rows and asserts v3 reads see `archivedAt = null`. |

**Sequence (dependency order)**

```
P0 (inventory) → P1 (C1) → P2 (C2) → P3 (H1) → P4 (H2) → P5 (H3)
                              ↓
                              P6 (H4 — AdminAuthViewModel wiring)
                P3, P4, P5, P7, P8 are pairwise independent
                P9 (M3) depends on P3
                P10 (M1,M2,M4,M5) depends on P3-P7
                P11 (L7) independent, runs after P10
                P12 (L1-L8 polish) independent
                P13 (docs + verify) gate
```

**Task tracker**: see [`TASKS.md`](./TASKS.md). 115 todos across 14 sub-phases.

**New files introduced** (deferred — file creation happens in the
matching phase):

- `app/src/main/java/com/example/data/model/Customer.kt` (1-line typealias, P1)
- `app/src/main/java/com/example/ui/viewmodel/AdminAuthViewModel.kt` (~120 lines, P2)
- `app/src/main/java/com/example/data/repository/ProductWriteResult.kt` (sealed class, P3)
- `app/src/main/java/com/example/data/repository/OutOfStockException.kt` (P5)
- `app/src/main/java/com/example/data/local/Migrations.kt` (`MIGRATION_2_3`, P4)
- `app/src/test/java/com/example/ui/viewmodel/AdminAuthViewModelTest.kt` (P2)
- `app/src/test/java/com/example/ui/viewmodel/ShopViewModelTest.kt` (P3)
- `app/src/test/java/com/example/data/repository/ProductRepositoryTest.kt` (P3)
- `app/src/test/java/com/example/data/local/MigrationTest.kt` (P4 — boots v2 schema, asserts v3 column)
- `app/src/test/java/com/example/data/repository/OrderRepositoryStockTest.kt` (P5)
- `app/src/androidTest/java/com/example/AdminPanelTest.kt` (P6)

**Modified files**:

- `AccountScreen.kt` — the entire admin surface (P1, P2, P3, P6, P7, P8, P9, P10, P11)
- `Product.kt` — add `archivedAt: Long?` (P4)
- `ProductDao.kt` (in `RoomComponents.kt`) — `getAllProductsFlow` adds `WHERE archivedAt IS NULL`, new `archiveProduct` / `unarchiveProduct` / `getArchivedProductsFlow` / `getStockDirect` / `decrementStock` / `restoreStock` (P4, P5)
- `AppDatabase` (in `RoomComponents.kt`) — schema v2 → v3, register `MIGRATION_2_3` (P4)
- `ShopViewModel.kt` — `addOrUpdateProduct` returns `Result<ProductWriteResult>` (P3); admin form passes discount fields (P9)
- `ProductRepository.kt` — `addOrUpdateProduct` returns `Result<ProductWriteResult>`; `syncProducts` keeps order history; new `archiveProduct` / `unarchiveProduct` / `getArchivedProducts` (P3, P4)
- `CartRepository.kt` — `addToCart` re-reads stock + throws `OutOfStockException` (P5)
- `OrderRepository.kt` — `placeCODOrder` re-reads stock inside `withTransaction` + throws `OutOfStockException` (P5)
- `ProductDetailScreen.kt` — Add-to-Cart button disabled at 0; new `OutOfStock` chip (P5)
- `AppViewModels.kt` + `AppViewModelsFactory.kt` — new `adminAuth: AdminAuthViewModel` field + factory branch (P6)
- `RemoteDatabaseService.kt` + `FirebaseDatabaseServiceImpl.kt` — new `suspend fun requireAdmin(): Result<Unit>` stub (P2)
- `Haptics.kt` — no new code, just usage in 5 sites (P8)
- `values/strings.xml` + `values-en/strings.xml` + `values-ar/strings.xml` — 12 new keys (P2, P5, P8)

**Verification surface** (per AGENTS.md — manual code review replaces `./gradlew`):

- Inline grep per phase (T0.4, T1.4, T2.20, T3.6, T4.13, T5.10, T6.5, T7.5, T8.7, T9.2, T10.13, T11.6)
- The Robolectric `MigrationTest` is the only automated gate; it does not need `./gradlew` to be authored and reviewed.
- The full `./gradlew test connectedDebugAndroidTest detekt lint assembleDebug` chain is deferred to Phase 7A (env-blocked).

**Status: SHIPPED** — all 14 sub-phases (P0–P13) committed across 7 atomic commits on `main`:
`a7ad3b4` (P1–P4 bundle, 21 files +1220/-111) →
`3bf9f77` (P5 H3 stock guard, 11 files +452/-13) →
`d5acd51` (P7 H5 Toast→MessageBus, 1 file +48/-16) →
`bec7c6c` (P8 H6 haptics on 7 admin onClicks, 1 file +38/-34) →
`ed2ea4d` (P9 M3 wire discount fields, 1 file +48/-1) →
`7464f09` (P10 M1/M2/L1/L4 polish, 4 files +75/-15) →
`1b5a9aa` (P12 L5 auth-flip Crossfade + L3 D8.15 doc, 2 files +29/-1).

23 audit findings: 2 Critical, 6 High, 5 Medium, 8 Low → all remediated.
Phase 6 (H4 auth-flip recreate) and Phase 11 (L7 AdminProductDraft refactor)
explicitly DEFERRED — rationale in `DECISIONS.md` (H4) and TASKS.md (L7).
See [`docs/admin-panel-audit.md`](./docs/admin-panel-audit.md) for the
audit summary, decision log, and pointers to per-phase commit hashes.

---

## 🌐 Future i18n Backlog (deferred — D2)

When Arabic + English LTR/RTL support is on the roadmap:

- [x] Add `values-en/strings.xml` (mirror of `values/strings.xml`) — **DONE**: 241 keys translated to English; XML well-formed; key set matches `values/strings.xml` exactly (verified via PowerShell `Compare-Object`). English translation uses semantic intent (e.g. `cd_close = "Close"` even though the Arabic value is a typo) so the English UI is correct. **Pre-existing Arabic typos found during translation** (deferred to Phase 8 to avoid behavior change):
  - `cd_close = "عودة"` (means "return/back") used for the X close button on the wishlist dialog — should be "إغلاق" (close). English version is correct.
  - `detail_offline_notice = "...مدعوم بالكامل للتخفيض والطلب..."` — "للتخفيض" (for discount) reads oddly; might be "للتخزين" (for storage). English version reads "supports discount and ordering" which matches either reading.
- [x] **D8.19 — Wire the in-app language switcher** — **DONE**: added `LocaleManager` utility (`attachBaseContext` wraps the base context with the persisted BCP-47 tag via `Context.createConfigurationContext`; no new dep, no `AppCompatActivity` swap). `MainActivity` exposes `setAppLocale(tag: String)` (persists + `recreate()`). `AccountScreen` shows a "Language" card mirroring the theme card, with two `LanguageOptionButton`s ("العربية" / "English") that highlight the active tag. The hardcoded `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` was dropped from `MainActivity`; layout direction is now derived from the active locale (Arabic = RTL, English = LTR). No new dependency, no manifest changes, no `android:configChanges` (the activity recreates normally on locale change). Default persisted tag is `"ar"`, so Arabic-system users see no change on first launch.
- [x] **D8.20 — `HomeTab` forward numbering** — **DONE**: flipped the enum to `HOME(0), PRODUCTS(1), DISCOUNTS(2), FAVORITES(3), ACCOUNT(4)`. The visual order in the `NavigationBar { ... }` block in `MainScreen.kt` is unchanged (Home first, Account last in source order) so the rendered tab order is still correct in both Arabic (RTL: Home on the right, Account on the left) and English (LTR: Home on the left, Account on the right). All `selected` checks, `onClick` handlers, and `when (HomeTab.fromIndex(selectedTab))` dispatch continue to work because they compare against `HomeTab.X.index` rather than literal numbers (audited: zero literal-index comparisons in `MainScreen.kt`). Stale `// (index 4)` / `// (index 0)` comments at the top/bottom nav items updated. `HomeTabTest.kt` added (4 tests: forward indices, declaration order, round-trip, fallback) to lock the contract.
- [x] **D8.21 — Locale-aware string resolution in VMs** — **DONE**: introduced `LocaleResources` singleton (`AtomicReference<Context>`) that the activity publishes its wrapped base context to in `MainActivity.attachBaseContext` (runs before `super.onCreate`, so the ref is set before any VM is constructed). Replaced all 16 `getApplication<Application>().getString(R.string.xxx)` call sites with `LocaleResources.getString(R.string.xxx)` across `CartViewModel` (1), `OrderViewModel` (9), `ShopViewModel` (4), and `UserProfileViewModel` (2). The previous `getApplication<...>()` pattern was the latent bug: the `Application` instance is created once per process and its base context is never re-wrapped, so the VM snackbar/toast strings would always be in the **default** locale (Arabic) — even after the user picked English via D8.19's in-app switcher. `LocaleResourcesTest.kt` added (3 Robolectric tests: empty fallback, Arabic vs English resolution, `%1$d` format args) to lock the contract. The atomic reference is updated on every `attachBaseContext` (i.e. on every activity recreate, including locale change), so VM coroutines that publish after a language switch resolve the new locale on the very next read.
- [x] Verify all hardcoded `LayoutDirection.Rtl` references in screens — **DONE**: grep audit returns 0 screen-level references. The only `LayoutDirection.Rtl` mention is a KDoc comment in `LocaleManager.kt:27` explaining the history (the hardcoded `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` was dropped from `MainActivity` in D8.19; layout direction is now derived from the active locale). No cleanup needed.
- [x] Bundle `.ttf` font assets as a 3rd-tier offline fallback (after Google Fonts + system) — **DONE**: downloaded IBM Plex Sans Arabic (Regular/Medium/SemiBold/Bold) + IBM Plex Sans (Regular/Medium/SemiBold/Bold) from Google Fonts GitHub repo into `res/font/` (~2.1 MB total). `FontFamilies.kt` updated: each `FontFamily` now has 3 tiers — Google Fonts (network) → bundled .ttf (offline, same font family) → system font (implicit fallback). The bundled fonts ensure the app looks identical offline vs online. OFL license text bundled at `res/raw/ofl_license.txt`; OFL attribution string already present in `values/strings.xml` (Phase 3.3).
- **WindowSizeClass, empty-state illustrations, real Firebase, FCM** — see **Phase 9** below for the planned D9.1–D9.4 work items. **D9.1 ✅, D9.2 ✅, D9.3 (scaffold) ✅, D9.4 (scaffold) ✅ are done; both are pending real Firebase wiring.**

---

## 📱 Phase 9 — Post-i18n enhancements

Four large work items that were deferred from Phase 6 (per D5) and
the i18n backlog, now planned as discrete D9.x work items. Each is
self-contained except D9.4 which depends on D9.3.

### Sequencing recommendation

> Do them in this order. It minimizes risk and keeps the user-visible
> payoff coming every step.

1. **D9.1 — WindowSizeClass** (do first). Pure Compose code, no
   external deps, no manifest changes, no backend. Smallest surface
   area, fastest to verify, biggest "feels professional" bump on
   tablets. Unblocks any future tablet UX work.
2. **D9.2 — Empty-state illustration slot** (do second). Pure code +
   a handful of placeholder vector drawables. Ships without a
   designer — placeholder art is fine; the slot is in place so a
   designer can drop in real art without touching any screen file.
3. **D9.3 — Real Firebase Auth + Firestore** (do third). Biggest
   blast radius: needs a Firebase project, `google-services.json`,
   `google-services` plugin, a new `FirestoreRemoteDatabaseService`,
   `ServiceLocator` rewiring, and proguard rules. Do this before
   D9.4 because D9.4 is meaningless without a real backend.
4. **D9.4 — FCM push notifications** (do last). Depends on D9.3.
   Adds `FirebaseMessagingService`, `POST_NOTIFICATIONS`
   permission, notification channel, and token plumbing.

### D9.1 — `WindowSizeClass` adaptive layouts for tablets ✅

- [x] `MainActivity.onCreate` calls `calculateWindowSizeClass(this)` and provides the result through a new `LocalWindowSizeClass` composition local (see `app/src/main/java/com/example/ui/locals/LocalWindowSizeClass.kt`). Mirrors the `LocalAppActivity` pattern from Phase 4. `staticCompositionLocalOf` chosen because the value is read by few composables and never changes during the activity's lifetime.
- [x] Replace `LazyVerticalGrid(GridCells.Fixed(2), ...)` in `ProductsScreen.kt`, `FavoritesScreen.kt`, and `DiscountsScreen.kt` with `GridCells.Adaptive(minSize = …)` where the minSize scales with the width class (180dp Compact / 220dp Medium / 260dp Expanded). `GridCells.Adaptive` is preferred over hard-coded column counts because the column count then derives from the actual width, not from a guess.
- [x] Branch `HomeScreen` carousels: extracted a private `ProductsCarousel` helper that picks `LazyRow` (with `width=175.dp` at Compact / `width=220.dp` at Medium) or `LazyVerticalGrid(GridCells.Fixed(2))` based on `WindowWidthSizeClass`. `ProductColumnItem` gained a `width: Dp? = 175.dp` parameter so the same composable works in both contexts.
- [x] Branch `AccountScreen` admin tabs: at `Expanded`, the Inventory (tab 1) and Customers (tab 2) tabs split into a `DraggableSplitRow` (8dp draggable splitter, 24dp grip pill, ratio coerced into `[0.2, 0.8]` plus a 200dp minimum per panel). The Add/Edit Product form (tab 0) stays full-width because it's too vertical to fit alongside another panel. Two new private composables — `AdminInventoryBody` and `AdminCustomersBody` — extracted so the same body renders in the left and right panel without duplication. Compact / Medium widths keep the original single-column if/else if/else if dispatch untouched.
- [x] New `LocalWindowSizeClass` defaults to a 360×640dp `WindowSizeClass` (Compact) so previews / unit tests can compose screens without an `Activity` context.
- [x] New dep: `androidx.compose.material3:material3-window-size-class` (BOM-managed, no explicit version) in `gradle/libs.versions.toml` + `app/build.gradle.kts`.

### D9.2 — Empty-state illustrations (designer asset) ✅

- [x] New `ui/components/EmptyState.kt`:
  ```kotlin
  @Composable
  fun EmptyState(
      illustration: Painter?,
      title: String,
      subtitle: String? = null,
      action: (@Composable () -> Unit)? = null,
  )
  ```
  - Illustration at top: `Image(painter, contentDescription = null)` (decorative — let the title do the talking for screen readers), 120dp tall, centered, with 24dp vertical breathing room
  - Title in `headlineSmall` 18sp ExtraBold, `onSurfaceVariant` at 70% alpha
  - Subtitle in `bodyMedium` 12sp, `onSurfaceVariant` at 60% alpha, max 2 lines, ellipsised
  - Optional action slot (used by the cart empty state for the "Browse store offerings" button)
- [x] 7 call sites converted (was originally written as 11 in the plan; audit found 7 distinct sites after merging the 3 admin panels and 4 user-facing screens):
  - `ProductsScreen.kt:245-268` (search empty → `empty_search_art`)
  - `DiscountsScreen.kt:130-137` (discounts empty → `empty_discounts_art`)
  - `FavoritesScreen.kt:91-125` (wishlist empty → `empty_wishlist_art`; previously used `Icons.Filled.Favorite` 64dp)
  - `CartScreen.kt:519+` (cart empty → `empty_cart_art` with Browse action button)
  - `AccountScreen.kt:1447` (orders empty, title only, `illustration = null`)
  - `AccountScreen.kt:1916` (inventory empty, title only, `illustration = null`)
  - `AccountScreen.kt:2025` (customers empty, title only, `illustration = null`)
- [x] 4 placeholder drawables created: `res/drawable/empty_search_art.xml`, `empty_discounts_art.xml`, `empty_wishlist_art.xml`, `empty_cart_art.xml` — minimal Material outline shapes (24×24 viewport, `fillColor="#FF000000"` so `ColorFilter.tint(outline@50%)` works). Each ships with an XML comment naming the artwork to drop in and the recommended canvas size (240×240dp, 1.5x density).
- [x] `values/strings.xml` + `values-ar/strings.xml` + `values-en/strings.xml` get `cd_empty_search_art` etc. content-descriptions only if the illustrations later become meaningful (not decorative) — for v1 they're decorative so no `cd_*` is needed
- [x] Document the designer hand-off: each placeholder drawable has an XML comment at the top naming the artwork to drop in and the recommended canvas size (240×240dp, 1.5x density)

### D9.3 — Real Firebase Auth + Firestore 🔲

> **External prerequisite**: user provides a Firebase project + a
> `google-services.json` for the dev/QA environment. The file is
> kept **out of git** (line 22 of `.gitignore`); production gets its
> own via CI secret.

- [x] Add `com.google.gms:google-services` plugin to `libs.versions.toml` (version `4.4.2`) + `build.gradle.kts` (root, `apply false`) + `app/build.gradle.kts` (conditionally applied via `if (file("google-services.json").exists())` so a fresh clone with no JSON still builds). Plugin ID: `com.google.gms.google-services`.
- [x] `google-services.json` is already in `.gitignore` (line 22). Documented in `RUN_GUIDE.md` (new "missing google-services.json" troubleshooting entry) and in the `app/build.gradle.kts` comment block.
- [x] `app/build.gradle.kts`: removed the commented `firebase-ai` reference; added `firebase-firestore-ktx` + `firebase-auth-ktx` (both BOM-managed, no explicit version). `firebase-bom` was already on the classpath.
- [x] New `data/remote/firebase/FirestoreRemoteDatabaseService.kt` implementing `RemoteDatabaseService` against `Firebase.firestore`. **Stub status**: each method body is a `TODO(D9.3)` returning `emptyList()` / `false` so the build succeeds and the contract is type-checked, but the app will appear empty when wired to the real backend. Real impls for each method are documented inline (e.g. `db.collection("products").get().await().toObjects(Product::class.java)`).
- [x] Anonymous auth scaffolding: `Firebase.auth` and `Firebase.firestore` are acquired at class init. The `signInGate: Mutex` is plumbed but the actual `signInAnonymously()` call will be added in the first commit that lands a real `FirestoreRemoteDatabaseService` method body. UID persistence (SharedPreferences vs DataStore) decided on the same commit.
- [x] `ServiceLocator.getRemoteService(context)` now returns `RemoteDatabaseService` (the contract, not the concrete class) and picks the impl by `BuildConfig.USE_FIREBASE`. `BuildConfig.USE_FIREBASE` is set in `buildTypes { ... }`: `false` in debug (in-memory impl + demo toggle), `true` in release (Firestore impl). The `RemoteDatabaseService` return type means repositories didn't need to change.
- [x] **Demo toggle decoupling**: introduced new `data/remote/NetworkSimulation.kt` interface with `setOnline(online: Boolean)` and `isOnline(): Boolean`. `FirebaseDatabaseServiceImpl` implements it (delegating to the existing `_isDemoOnline` state); `FirestoreRemoteDatabaseService` does NOT. `ServiceLocator.getNetworkSimulation(context): NetworkSimulation?` returns non-null only when the in-memory impl is active. `OrderViewModel.toggleDemoConnectivity()` calls into the simulation; when it's `null` (Firestore build) the toggle is a no-op and shows a `msg_demo_unavailable` snackbar.
- [x] `proguard-rules.pro`: added the Firebase keep rules — `-keep class com.google.firebase.** { *; }`, `-keep class com.google.android.gms.** { *; }`, `-keepattributes Signature`, `-keepattributes *Annotation*`, plus the `@Keep` annotation + member preserves.
- [ ] `Product`, `Order`, `UserProfile` get `@PropertyName` annotations only if Firestore's default field-naming rules mismatch the data classes. (Currently `nameAr`, `descriptionAr`, etc. are already camelCase and Firestore will accept them as-is — verify on first commit.) 🔲 — pending the first real Firestore method body.
- [ ] New unit test: `ServiceLocator` returns the right impl for each `BuildConfig.USE_FIREBASE` value. 🔲 — blocked on `gradlew` (Phase 7A).

### D9.4 — FCM push notifications 🟡 (scaffolded, pending real backend)

> **Prerequisite**: D9.3 scaffold landed (`28b6753`). FCM tokens
> are useless without a real backend to send to — the
> `FirestoreRemoteDatabaseService.uploadDeviceToken` body is a
> `TODO(D9.4)` and the demo in-memory impl no-ops.
>
> **External prerequisite** (same as D9.3): user provides
> `google-services.json`. Without it, `BuildConfig.USE_FIREBASE`
> stays `false` in debug, the FCM service is never instantiated,
> and no notification is ever posted.

- [x] `AndroidManifest.xml`:
  - Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (Android 13+)
  - Declared `<service android:name=".fcm.FcmService" android:exported="false">` with `com.google.firebase.MESSAGING_EVENT` intent filter
- [x] New `fcm/FcmService.kt` extending `FirebaseMessagingService`:
  - `onNewToken(token)`: persist to `SharedPreferences` (`fcm_prefs/device_token`) and forward to `RemoteDatabaseService.uploadDeviceToken(token)` via `ServiceLocator`
  - `onMessageReceived(message)`: build a `NotificationCompat.Builder` on the `ecity_orders` channel and `NotificationManagerCompat.notify(...)`. Title/body default to `notification_default_title`/`notification_default_body` if the payload doesn't include them. Title/body resolve via `LocaleResources` so the notification matches the active locale (D8.21 invariant)
  - Channel ID: `ecity_orders` (constant on `FcmService.CHANNEL_ORDERS_ID`; re-used by `MainActivity` when creating the channel)
- [x] `MainActivity.onCreate`: create the `ecity_orders` channel via `createOrdersNotificationChannel()` and launch `fetchAndUploadFcmToken()` in `lifecycleScope`. Both are gated on `BuildConfig.USE_FIREBASE` so debug builds without a JSON don't pollute system settings with a channel the app will never use.
- [x] POST_NOTIFICATIONS request: new top-level composable `NotificationPermissionEffect` in `MainActivity.kt` that uses `rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)` and calls `launchPermissionRequest()` once per process. Wrapped in `if (BuildConfig.USE_FIREBASE)` inside `setContent`.
- [x] Uncommented `implementation(libs.accompanist.permissions)` in `app/build.gradle.kts`.
- [x] Added `firebase-messaging-ktx` to `libs.versions.toml` (BOM-managed). Pulls in `kotlinx-coroutines-play-services` transitively for `Task<T>.await()`.
- [x] `RemoteDatabaseService` contract: added `suspend fun uploadDeviceToken(token: String): Boolean`. The in-memory impl returns `true` (no-op); the Firestore stub is `TODO(D9.4)`.
- [x] `values/strings.xml` + `values-ar/strings.xml` + `values-en/strings.xml` got 5 new keys under the `notification_*` namespace: `notification_channel_orders_name`, `notification_channel_orders_desc`, `notification_default_title`, `notification_default_body`, `notification_permission_rationale`.
- [x] Proguard: added FCM-specific keep rules pinning `com.example.fcm.FcmService` + its companion, so the manifest's `<service android:name=".fcm.FcmService">` survives R8 shrinking. The Firebase-SDK keeps from D9.3 still cover `FirebaseMessagingService` and `RemoteMessage`.
- [ ] **Manual test** 🔲 — blocked on the same `google-services.json` + `BuildConfig.USE_FIREBASE = true` switch. Once both land, send a test message from the Firebase console and verify the device receives a system notification with the right title/body in both Arabic and English.
