package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the bottom-tab selection and a small back stack. Kept as a plain
 * (non-Android) [ViewModel] because the state has no Context or repo
 * dependencies.
 */
class NavigationViewModel : ViewModel() {
    private val _selectedTab = MutableStateFlow(HomeTab.HOME.index)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val tabHistory = ArrayDeque<Int>()

    fun selectTab(index: Int) {
        if (_selectedTab.value != index) {
            tabHistory.addLast(_selectedTab.value)
            _selectedTab.value = index
        }
    }

    fun selectTab(tab: HomeTab) = selectTab(tab.index)

    fun selectTabDirectly(index: Int) {
        _selectedTab.value = index
    }

    fun selectTabDirectly(tab: HomeTab) = selectTabDirectly(tab.index)

    fun popTabHistory(): Int? {
        if (tabHistory.isEmpty()) return null
        return tabHistory.removeLast()
    }
}
