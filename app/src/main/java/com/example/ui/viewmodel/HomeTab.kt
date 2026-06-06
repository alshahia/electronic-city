package com.example.ui.viewmodel

/**
 * Bottom-tab identifiers with **forward** numeric indexing (D8.20).
 *
 * The `index` field matches the natural LTR list ordering and the
 * `entries[i]` declaration order:
 *
 * - HOME is index 0
 * - ACCOUNT is index 4
 *
 * Historically these were reverse-numbered (HOME=4, ACCOUNT=0) so that
 * the value doubled as the "visual position from the left in RTL". With
 * D8.19 the layout direction now derives from the locale, so the visual
 * position in the `NavigationBar` is governed by source order + locale
 * (not by these magic numbers). Switching to forward indexing makes
 * `entries.firstOrNull { it.index == i }` and `when (i)` checks read
 * naturally, and it lines up with the LTR convention used by every
 * other Android bottom-nav.
 *
 * The visual order of the tabs in the `NavigationBar { ... }` block
 * in `MainScreen.kt` is unchanged (Home first, Account last in source
 * order), so the rendered tab order is correct in both Arabic (RTL:
 * Home on the right, Account on the left) and English (LTR: Home on
 * the left, Account on the right). Only the `index` field values
 * change; the `selected` checks, `onClick` handlers, and
 * `when (HomeTab.fromIndex(selectedTab))` dispatch all keep working
 * because they compare against `HomeTab.X.index` rather than literal
 * numbers.
 */
enum class HomeTab(val index: Int) {
    HOME(0),
    PRODUCTS(1),
    DISCOUNTS(2),
    FAVORITES(3),
    ACCOUNT(4);

    companion object {
        /** Reverse lookup; falls back to HOME for any unknown index. */
        fun fromIndex(index: Int): HomeTab = entries.firstOrNull { it.index == index } ?: HOME
    }
}
