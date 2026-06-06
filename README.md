<div align="center">
<img width="1200" height="475" alt="Electronic City" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# إلكترونك-سيتي — Electronic City

A single-Activity, MVVM Android e-commerce app for the Iraqi electronics
market. Arabic-first (RTL) with a Cash-On-Delivery checkout flow,
offline-first order queue, and a developer/admin module.

- **Min SDK / Target SDK:** see `app/build.gradle.kts`
- **Language:** Kotlin 2.2.x, Jetpack Compose
- **Build tool:** Gradle 9.3.1 (locally pinned in `.gradle_dist/`)
- **Persistence:** Room 2.7.x
- **DI:** manual `ServiceLocator` (D7)
- **Static analysis:** detekt 1.23.7 + Android Lint (lint failures break release)

---

## 📑 Project map

| Doc | What's in it |
| --- | --- |
| [`AGENTS.md`](./AGENTS.md) | Required reading order for AI agents + working agreements |
| [`plan.md`](./plan.md) | Phased refactor task list (0 → 7) + future i18n backlog |
| [`DECISIONS.md`](./DECISIONS.md) | Locked architectural choices (D1 → D8.17) |
| [`RUN_GUIDE.md`](./RUN_GUIDE.md) | Step-by-step build/deploy/troubleshoot |
| [`.context/`](./.context/) | Untracked context files (skills guide, validation prompt) |

---

## 🛠️ Build, test, analyze

The project has **no `gradlew` wrapper** in-repo and the dev machine may
not have Java/Gradle on `PATH`. All commands assume the locally pinned
Gradle 9.3.1 toolchain — see [`RUN_GUIDE.md`](./RUN_GUIDE.md) for setup.

### Debug build
```bash
.gradle_dist/gradle-9.3.1/bin/gradle.bat assembleDebug
```

### Static analysis
```bash
# detekt (style, complexity, potential bugs; warnings break the build)
.gradle_dist/gradle-9.3.1/bin/gradle.bat detekt

# Android Lint (treats warnings as errors for release builds)
.gradle_dist/gradle-9.3.1/bin/gradle.bat lint
```

### Unit + instrumented tests
```bash
.gradle_dist/gradle-9.3.1/bin/gradle.bat test
.gradle_dist/gradle-9.3.1/bin/gradle.bat connectedDebugAndroidTest
```

### Release build (R8 + resource shrinking)
```bash
# Required env vars (no silent fallback — build aborts if missing):
#   KEYSTORE_PATH   absolute path to the .jks
#   STORE_PASSWORD  keystore password
#   KEY_PASSWORD    key password
#   KEY_ALIAS       key alias (optional, defaults to "key0")
.export .env.local   # or set in your shell
.gradle_dist/gradle-9.3.1/bin/gradle.bat assembleRelease
```

`isMinifyEnabled = true` and `isShrinkResources = true` are on for
release. See [`proguard-rules.pro`](./proguard-rules.pro) for keep rules.

---

## 📦 Project layout

```
app/
  build.gradle.kts                 # R8, detekt, lint, signing config
  lint-baseline.xml                # empty placeholder
  proguard-rules.pro               # R8 keep rules
  src/main/
    AndroidManifest.xml
    java/com/example/
      MainActivity.kt              # single Activity, hosts AppRoot
      data/
        model/                     # Room entities + DTOs
        remote/                    # FirebaseDatabaseService interface + impl
        ServiceLocator.kt          # D7: manual DI for repos
      ui/
        AppViewModels.kt           # bundle of 6 Activity-scoped VMs
        AppRoot.kt                 # top-level Scaffold + NavHost glue
        haptics/                   # Haptics.kt (LongPress helpers)
        screens/                   # MainScreen, Home, Products, Discounts,
                                   # Favorites, Cart, ProductDetail, Account
        theme/                     # Color.kt, Type.kt, FontFamilies.kt
        viewmodel/                 # 6 focused VMs
    res/
      values/strings.xml           # Arabic default
      values-ar/strings.xml        # explicit Arabic mirror
      values-en/                   # slot reserved (D2 future i18n)
      drawable/                    # ic_product_placeholder/error
config/detekt/
  detekt.yml                       # project-tuned static analysis config
  detekt-baseline.xml              # empty placeholder
```

---

## 🌍 i18n status

Per D2 + D8.14:
- `values/strings.xml` is the Arabic default
- `values-ar/strings.xml` is an explicit Arabic mirror (same content)
- `values-en/` slot is reserved; English CD values (`cd_back = "Back"`,
  `cd_search = "Search"`, etc.) live in `values/` until the proper
  English file is introduced

See `plan.md` → "Future i18n Backlog" for the full list of work
gated on `AppCompatDelegate.setApplicationLocales(...)`.

---

## 📊 Refactor status

- **Phases 0 → 6:** complete (commit history walks the refactor)
- **Phase 7:** split into 7A (build verification, **blocked** until
  `gradlew`/Java is on `PATH`) and 7B (code review & docs, in progress)
- **Phase 8+:** see `plan.md` → "Future i18n Backlog" and D2

The current tip is at the head of `main`; pushes to `origin/main` are
blocked (no write perms on the configured remote) and are verified
locally with `git status` + `git log` instead.
