package com.example.data.model

/**
 * D8.22 — `AdminCustomersBody(customers: List<Customer>, …)` at
 * `AccountScreen.kt:1994` was a real compile break: the wildcard import
 * `com.example.data.model.*` (line 30) does not bring in any [Customer]
 * class. The minimum-disruption fix is this [typealias]; zero call-site
 * changes (the call site at `AccountScreen.kt:1439-1444` already passes
 * `vms.userProfile.onlineCustomers: StateFlow<List<UserProfile>>`).
 *
 * The typealias is forward-compatible: if a future `Customer` model with
 * extra fields is ever introduced, this typealias gets replaced by a
 * real `data class Customer(...)` and the compiler flags the call sites
 * that need to migrate.
 */
typealias Customer = UserProfile
