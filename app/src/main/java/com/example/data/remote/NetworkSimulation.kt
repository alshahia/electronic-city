package com.example.data.remote

/**
 * D9.3 — Demo-only network simulation hook. Lets the in-app "Toggle
 * connectivity" admin action flip a software flag that the
 * [RemoteDatabaseService.isOnlineFlow] respects, so the demo can
 * simulate offline behavior without a real network drop.
 *
 * Production backends (Firestore) don't implement this — their
 * online state is real `ConnectivityManager` data. [ServiceLocator]
 * returns `null` for the simulation when a real backend is active,
 * so VMs that call into the simulation are forced to no-op.
 */
interface NetworkSimulation {
    suspend fun setOnline(online: Boolean)
    fun isOnline(): Boolean
}
