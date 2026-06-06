package com.example.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the [HomeTab] forward-indexing contract (D8.20).
 *
 * If anyone reverts to the historical reverse indexing (HOME=4, ACCOUNT=0)
 * or shifts the declaration order, the bottom-nav dispatch in
 * `MainScreen.kt` will silently route the user to the wrong screen, so
 * the contract is worth pinning down with explicit assertions.
 */
class HomeTabTest {
    @Test
    fun indices_areForwardNumbered() {
        assertEquals(0, HomeTab.HOME.index)
        assertEquals(1, HomeTab.PRODUCTS.index)
        assertEquals(2, HomeTab.DISCOUNTS.index)
        assertEquals(3, HomeTab.FAVORITES.index)
        assertEquals(4, HomeTab.ACCOUNT.index)
    }

    @Test
    fun indices_matchDeclarationOrder() {
        val declared = HomeTab.entries.map { it.index }
        assertEquals(listOf(0, 1, 2, 3, 4), declared)
    }

    @Test
    fun fromIndex_roundTripsAllEntries() {
        HomeTab.entries.forEach { tab ->
            assertEquals(tab, HomeTab.fromIndex(tab.index))
        }
    }

    @Test
    fun fromIndex_unknownFallsBackToHome() {
        assertEquals(HomeTab.HOME, HomeTab.fromIndex(-1))
        assertEquals(HomeTab.HOME, HomeTab.fromIndex(99))
        assertEquals(HomeTab.HOME, HomeTab.fromIndex(Int.MIN_VALUE))
    }
}
