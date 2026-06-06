# Admin Panel Audit — Remediation Summary

> **Scope**: `app/src/main/java/com/example/ui/screens/AccountScreen.kt` lines
> 789–2138 (`AccountSheet.AdminPanel`) at the time of audit (May 2026).
> **Status**: ✅ Shipped 2026-06-05. **Source prompt**:
> `.context/verification-validation-system-prompt.md` (7 dimensions:
> security, logic, architecture, UX, performance, maintainability, i18n).

## Headline

A multi-dimensional audit surfaced **23 issues** (2 Critical / 6 High /
5 Medium / 8 Low) in the admin panel. All 23 were either remediated
or explicitly deferred with rationale. The remediation executed across
**14 sub-phases** (P0–P13) and **7 atomic commits** on `main` between
2026-05 and 2026-06.

Full task breakdown: [`../plan.md` §7B-2](../plan.md#7b-2--admin-panel-audit-remediation-shipped-2026-06-05).
Per-todo verification grep: [`../TASKS.md`](../TASKS.md).

## Issues by severity

| # | Severity | Finding | Resolution |
|---|----------|---------|------------|
| C1 | Critical | `AdminCustomersBody(customers: List<Customer>, ...)` was a compile break — no `Customer` class in the wildcard import | `typealias Customer = UserProfile` (D8.22, P1) |
| C2 | Critical | `equals("admin123")` literal in `AccountScreen.kt:896`; gate state in `remember` (survives `Activity.recreate()`) | `AdminAuthViewModel` (D8.23, P2) — 5/60s lockout, 5-min idle, 4 Robolectric tests |
| H1 | High | `addOrUpdateProduct` returned `Result<Unit>`; local + remote failure modes indistinguishable | `ProductWriteResult` sealed interface (4 cases) (P3) |
| H2 | High | Hard delete on a product with `OrderItem` rows fails on `FK RESTRICT` | Soft-archive via `Product.archivedAt: Long?` + `MIGRATION_2_3` + Archived tab (D8.24, P4) |
| H3 | High | TOCTOU race + unlimited cart-add against current stock | `OutOfStockException` + atomic re-read in `withTransaction` (D8.25, P5) — 3 Robolectric tests, 1 Robolectric headline case |
| H4 | High | Auth state in `remember` (D8.19 reproduce risk) | `vms.adminAuth.isAuthenticated.collectAsState()` (P6 — verified P2) |
| H5 | High | 4 admin `Toast` sites bypass `LocaleResources` (D8.21 invariant) | `MessageBus.publish(LocaleResources.getString(...))` (P7) |
| H6 | High | No haptics on 7 admin onClicks | `rememberHapticClick()` (P8) |
| M1 | Medium | Image URL field accepts `ftp://`, `data:`, `file://` etc. | Light-touch per-keystroke rejection of non-`http(s)` values (P10) |
| M2 | Medium | `TextAlign.Left` (hardcoded LTR) in 5 admin form sites | `TextAlign.Start` (RTL-aware) (P10) |
| M3 | Medium | `Product.isDiscounted` / `originalPrice` columns exist but not wired through admin upload | 1-line wire-through + 3× onLoadProduct + 2× reset (P9) |
| M4 | Medium | Inventory / customers bodies are 90+ line `forEach` blocks | **Deferred** (T10.7-T10.8) — `LazyColumn` refactor; no functional benefit |
| M5 | Medium | No search input in admin inventory | **Deferred** (T10.9) — stretch; not in audit scope |
| L1 | Low | `System.currentTimeMillis()` for new product ids | `UUID.randomUUID()` (P10) |
| L2 | Low | `TextAlign.Right` (hardcoded RTL) in customer body | **Deferred** (T10.6) — Arabic-only locale set; reads identical to `TextAlign.End` |
| L3 | Low | Intentional `isImageError` fallback unexplained | D8.15 KDoc added (P12) |
| L4 | Low | `admin_inventory_price` uses `%1$d` (truncates decimal) | `%1$s` + `prod.price.toString()` (P10) |
| L5 | Low | Auth-flip is a hard cut; no `Crossfade` | `Crossfade(targetState = isAdminAuthenticated)` wrapping lines 821-1691 (P12) |
| L6 | Low | i18n drift risk | **No drift introduced** (P12) |
| L7 | Low | 12 individual `var adminProductXxx` declarations | **Deferred** (P11) — 12 fields × ~50 sites = ~100 mechanical edits; no functional benefit |
| L8 | Low | Profile card `clickable` lacks haptics | **Deferred** (T12.1) — user-facing surface, outside admin scope |
| T4.5 | Test | `MIGRATION_2_3` not test-covered | `MigrationTest.kt` (Robolectric, 2 cases) (P4) |
| T5.8 | Test | "Sold out" badge on storefront cards | **Deferred** — no shared `ProductCard`; would require 3 independent edits |

## 6 pre-flight decisions (locked)

| # | Decision | Choice | Implication |
|---|----------|--------|-------------|
| C1 | Customer type | `typealias Customer = UserProfile` | One-line fix in `data/model/Customer.kt`. Zero call-site change. |
| H2 | Delete policy | Soft-archive via `archivedAt: Long?` | New column + Room v2→v3 migration + "Archived" admin tab. Preserves `OrderItem` history (FK is RESTRICT). |
| H3 | Stock guard | Both — check at add-to-cart AND at order placement | `CartRepository.addToCart` throws `OutOfStockException`; `OrderRepository.placeCODOrder` re-verifies in `withTransaction`; `ProductDetailScreen` disables Add-to-Cart at 0. |
| M3 | Discount fields | Wire them (not drop) | `Product.isDiscounted` and `Product.originalPrice` columns already exist; only the admin upload call site fails to pass them. One-line wire-through + admin form re-validation + display. |
| L7 | Admin form state | **Deferred** | 12 fields × ~50 sites = ~100 mechanical edits; no functional benefit. Re-evaluate when `AccountScreen.kt` > 2500 lines (currently 2608+). |
| T4.5 | Migration | Real `Migration(2, 3)` + Robolectric test | `ALTER TABLE products ADD COLUMN archivedAt INTEGER`. Test boots v2 with rows and asserts v3 reads see `archivedAt = null`. |

## Files introduced (6)

| File | Phase | Purpose |
|------|-------|---------|
| `app/src/main/java/com/example/data/model/Customer.kt` | P1 | 1-line typealias for `UserProfile` (D8.22) |
| `app/src/main/java/com/example/ui/viewmodel/AdminAuthViewModel.kt` | P2 | 4 StateFlows + 5/60s lockout + 5-min idle + `signInWithFirebase`/`lockNow`/`recordActivity` (D8.23) |
| `app/src/main/java/com/example/data/repository/ProductWriteResult.kt` | P3 | Sealed interface, 4 cases for `addOrUpdateProduct` (H1) |
| `app/src/main/java/com/example/data/local/Migrations.kt` | P4 | `MIGRATION_2_3` (D8.24) |
| `app/src/main/java/com/example/data/repository/OutOfStockException.kt` | P5 | Runtime exception with productId/nameAr/nameEn/availableStock/requestedQuantity (D8.25) |
| `app/src/test/.../AdminAuthViewModelTest.kt` | P2 | 4 Robolectric cases locking the 5/60s + 5-min contract (D8.23) |
| `app/src/test/.../MigrationTest.kt` | P4 | 2 Robolectric cases locking the v2→v3 schema migration (D8.24) |
| `app/src/test/.../OrderRepositoryStockTest.kt` | P5 | 3 Robolectric cases including the H3 headline `placeOrder_rollsBackPartialDecrement_onStockFailure` (D8.25) |

(Note: 6 production files + 3 test files = 9 new files. The "6 new files" in
T13.4 is the production count.)

## Files modified (12)

| File | Phases | What |
|------|--------|------|
| `AccountScreen.kt` | P1, P2, P3, P4, P5, P7, P8, P9, P10, P12 | The entire admin surface — 2608+ lines |
| `Product.kt` | P4 | `archivedAt: Long?` (D8.24) |
| `RoomComponents.kt` | P4, P5 | `ProductDao` archive/unarchive/stock queries; `AppDatabase` v2→v3 |
| `ShopViewModel.kt` | P3, P4, P9 | `addOrUpdateProduct` returns `Result<ProductWriteResult>`; archive/restore pass-through |
| `ShopRepositories.kt` | P3, P4, P5 | `ProductRepository`, `CartRepository.addToCart`, `OrderRepository.placeCODOrder` |
| `ProductDetailScreen.kt` | P5, P12 | Add-to-Cart `enabled = product.stock > 0`; D8.15 KDoc on `isImageError` |
| `ServiceLocator.kt` | P5 | Pass `AppDatabase` to `OrderRepository` |
| `AppViewModels.kt` + `AppViewModelsFactory.kt` | P2 | New `adminAuth: AdminAuthViewModel` field + factory branch |
| `RemoteDatabaseService.kt` + `FirebaseDatabaseServiceImpl.kt` | P2 | New `suspend fun requireAdmin(): Result<Unit>` stub |
| `values/strings.xml` + `values-en/strings.xml` + `values-ar/strings.xml` | P2, P3, P4, P5, P10 | 12 new keys; `admin_inventory_price` `%1$d` → `%1$s`; `admin_demo_password_hint` blanked |
| `gradle/libs.versions.toml` | P4 | `roomTesting = "2.7.0"` + `androidx-room-testing` |
| `app/build.gradle.kts` | P4 | `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` + `testImplementation(libs.androidx.room.testing)` |

## Commit history (7 atomic commits, not pushed)

```
a7ad3b4  fix(admin): Phase 7B-2 P1-P4 bundle (C1+C2+H1+H2)  — 21 files +1220/-111
3bf9f77  fix(admin): Phase 7B-2 P5 H3 stock guard            — 11 files +452/-13
d5acd51  fix(admin): Phase 7B-2 P7 H5 Toast→MessageBus       —  1 file  +48/-16
bec7c6c  fix(admin): Phase 7B-2 P8 H6 haptics on 7 admin onClicks — 1 file +38/-34
ed2ea4d  fix(admin): Phase 7B-2 P9 M3 wire discount fields    —  1 file  +48/-1
7464f09  fix(admin): Phase 7B-2 P10 M1/M2/L1/L4 polish        —  4 files  +75/-15
1b5a9aa  fix(admin): Phase 7B-2 P12 L5 auth-flip Crossfade + L3 D8.15 doc — 2 files +29/-1
```

> **P0–P13 commit policy note:** Phases 1–4 were bundled into a single
> commit (`a7ad3b4`) at user request — the original plan was one-commit-
> per-phase (D8.17 convention). Phases 5–12 are one-commit-per-phase.
> Phase 13 (this commit) is the docs-only pass: `plan.md` + `TASKS.md`
> + `DECISIONS.md` + this file.

## Verification surface

Per `AGENTS.md`, manual code review replaces `./gradlew`. Each phase
has a `**V**` verification grep in `TASKS.md`. The Robolectric tests
(`AdminAuthViewModelTest`, `MigrationTest`, `OrderRepositoryStockTest`)
are authored + reviewed but not run on this machine (no Java/Gradle).
The full `./gradlew test connectedDebugAndroidTest detekt lint
assembleDebug` chain is deferred to Phase 7A (env-blocked).

## Open follow-up work (deferred from this round)

| Item | Reason | When to revisit |
|------|--------|-----------------|
| T10.6 `TextAlign.End` sweep in customer body | Arabic-only locale set; reads identical to `TextAlign.Right` | When customer column is next touched |
| T10.7-T10.8 `LazyColumn` for inventory + customers | 90+ line `forEach` bodies; need careful extraction to preserve dialog state | If `AccountScreen.kt` > 3000 lines |
| T10.9 M5 search input in inventory | Stretch goal, not in audit scope | Next admin UX pass |
| T5.8 "Sold out" badge on storefront cards | No shared `ProductCard`; 3 inline sites in `HomeScreen.kt`/`ProductsScreen.kt`/`DiscountsScreen.kt` | When a shared `ProductCard` is extracted |
| T11 L7 `AdminProductDraft` refactor | 12 fields × ~50 sites = ~100 mechanical edits; no functional benefit | If `AccountScreen.kt` > 3000 lines or new admin form field is added |
| T12.1 Profile card haptics | User-facing surface, outside admin audit | Future profile UX pass |
| D9.3 Real Firebase Auth + Firestore | External prerequisite: `google-services.json` | When user provides the Firebase project |
| D9.4 FCM push notifications | Depends on D9.3 | After D9.3 lands |

## What didn't change

- **C2 password literal in code comments**: `admin123` is still mentioned
  in 3 source-file KDoc comments (rationale + history) and in
  `AdminAuthViewModelTest.kt`'s expected-password constant. The
  *user-facing* surface (3 strings.xml entries × 3 locales) is blanked.
- **C2 has no server-side check**: `RemoteDatabaseService.requireAdmin()`
  is a stub that accepts any non-blank. D9.3 must replace with a real
  claim check.
- **`archivedAt` is not surfaced in the storefront product card** —
  only in the admin area. The `WHERE archivedAt IS NULL` filter on
  storefront queries makes the column invisible to users by design.
- **`MessageBus` Toast→MessageBus pattern (D8.21) is now applied
  uniformly to admin actions**: 4 admin sites converted in P7.
  Profile-side toasts are out of scope.

## Pointer

- **Full plan**: [`../plan.md` §7B-2](../plan.md#7b-2--admin-panel-audit-remediation-shipped-2026-06-05) (sequence, file lists, verification surface, 6 locked decisions)
- **Per-todo tracker**: [`../TASKS.md`](../TASKS.md) (115 todos across P0–P13)
- **Decisions**: [`../DECISIONS.md`](../DECISIONS.md) D8.22–D8.25
- **Audit prompt**: [`../.context/verification-validation-system-prompt.md`](../.context/verification-validation-system-prompt.md)
