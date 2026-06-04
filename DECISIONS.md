# Locked Decisions — إلكترونك-سيتي Refactor

This file records the architectural and implementation decisions for the
إلكترونك-سيتي refactor. Every choice here is **deliberate**; deviations
should be discussed in a PR and updated here.

---

## D1 — Persistence Library: Room ✅

We stay on **Room** (Google, KSP codegen). Rationale:
- Already in production use, 4 entities + 4 DAOs working
- Android-only product, no iOS KMP target
- Library switch during a schema refactor is a "two big changes" anti-pattern
- Re-evaluate only if a shared `:data` module or KMP becomes a roadmap item

## D2 — Tab Numbering: Keep reverse, plan for LTR/RTL i18n 🔁

We **keep reverse indexing** (Home=4, Account=0) during this refactor — zero
behavior change, no RTL layout risk. A future task adds proper i18n:

- `values-ar/strings.xml` (existing Arabic resources)
- `values-en/strings.xml` (new English translation)
- `Locale` driven `LocalLayoutDirection` (no reverse indexing)
- `enum class HomeTab(val index: Int)` becomes the source of truth
- `MainActivity` picks layout direction from `AppCompatDelegate.getApplicationLocales()`

**Future work tracked in:** `plan.md` § Future i18n Backlog.

## D3 — Font Family: Google Fonts with layered fallback 🔤

We use **Google Fonts** via `androidx.compose.ui:ui-text-google-fonts`, with
a layered fallback chain so the app renders correctly on first run with no
network:

1. **First choice:** Google Fonts (IBM Plex Sans Arabic + IBM Plex Sans)
2. **Second choice:** System font (FontFamily.Default / FontFamily.SansSerif)
3. **Third choice (deferred):** Bundled `.ttf` assets in `res/font/`

The provider is `GoogleFont.Provider` with `com.google.android.gms.fonts`
authority; Compose caches the font after first successful fetch, so the
fallback only triggers once per device.

**Configuration strategy:**

```kotlin
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val DisplayFont = FontFamily(
    Font(GoogleFont("IBM Plex Sans Arabic"), provider, FontWeight.Bold),
    Font(GoogleFont("IBM Plex Sans Arabic"), provider, FontWeight.SemiBold)
)
val BodyFont = FontFamily(
    Font(GoogleFont("IBM Plex Sans Arabic"), provider, FontWeight.Normal)
)
```

Compose's `Font` API takes a `fontProvider` argument; if the fetch fails it
falls back to the next item in the `FontFamily` chain (which we will set to
the system font), giving a graceful offline first-run.

## D4 — Static Analysis: detekt ✅

We adopt **detekt** (1.23.x) with `config/detekt.yml` and a baseline.
Rationale:
- Catches real code smells (LongMethod, TooManyFunctions, MagicNumber) that
  the review identified
- Built-in rule sets: `style`, `complexity`, `exceptions`, `performance`,
  `naming`, `potential-bugs`
- Kotlin-first, easy CI integration
- Customizable per project

ktlint was rejected because it only does style enforcement, not bug detection.

## D5 — Phase 6 UX Polish Scope: subset ✅

We ship in v1:

- **6.1** Show `ProductsUiState.Error` as a retry card (not silent empty)
- **6.4** Haptic feedback on add-to-cart and toggle-favorite
- **6.5** Extract hardcoded strings to `values-ar/strings.xml`
- **6.8** Add Coil `placeholder` and `error` drawables

Deferred to v2:

- 6.2 Empty-state illustrations (needs designer)
- 6.3 WindowSizeClass adaptive layouts (needs tablet launch)
- 6.6 Real Firebase Auth + Firestore (separate infrastructure project)
- 6.7 FCM push notifications (separate infrastructure project)

## D6 — Legacy Files: archive, not delete 🗂️

- `Verification & Validation System Prompt.md` → `.context/`
- `metadata.json` → `docs/legacy-ai-studio/`
- Both moves are reversible; READMEs explain purpose

## D7 — ViewModel Scoping: 6 Activity-scoped focused VMs ✅

We split the God `ECommerceViewModel` (426 lines, 8 responsibilities) into
**6 focused ViewModels**, all scoped to `MainActivity`'s
`viewModelStoreOwner`:

1. `ThemeViewModel` — `themeMode`, `setThemeMode(...)`
2. `UserProfileViewModel` — user profile + online customers
3. `ShopViewModel` — products, filters, `ProductsUiState`, detail selection
4. `CartViewModel` — cart items, total, count
5. `OrderViewModel` — orders, COD checkout, offline sync
6. `NavigationViewModel` — tab history, back stack

A small `AppViewModels` data class bundles them so screens can request
exactly the subset they need. If a future `NavHost` adds a real checkout
destination, that one VM will be scoped to its own `NavBackStackEntry`.

## D8 — Additional refactor decisions made during execution

- **D8.1** `Order.itemsJson: String` → proper `OrderItem` entity + `@Relation`
- **D8.2** `CartRepository.addToCart` → O(1) `getQuantity` query
- **D8.3** `MyApplicationTheme` → wrap in `remember(darkTheme)`
- **D8.4** Sync snackbar `NeutralDark` → `MaterialTheme.colorScheme.inverseSurface`
- **D8.5** R8 enabled for release; proguard rules for Room + Moshi
- **D8.6** `KEYSTORE_PATH` env var required (no silent fallback)
- **D8.7** `LocalAppActivity` composition local replaces `ContextWrapper` walk
- **D8.8** Network badge 8sp → 11sp (Material label minimum)
- **D8.9** `surfaceVariant` light value fixed to `#EEF3E6` (was same as bg)

---

Last updated: Phase 0
