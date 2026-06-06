package com.example.data.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.remote.NetworkSimulation
import com.example.data.remote.RemoteDatabaseService
import com.example.data.remote.firebase.FirebaseDatabaseServiceImpl
import com.example.data.remote.firebase.FirestoreRemoteDatabaseService
import com.example.data.repository.CartRepository
import com.example.data.repository.FavoriteRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.ProductRepository

/**
 * ServiceLocator acts as our lightweight, scalable dependency injection container.
 * This guarantees consistent singleton lifetimes and clean modular upgrades.
 */
object ServiceLocator {
    private var database: AppDatabase? = null
    private var remoteService: RemoteDatabaseService? = null

    private var productRepo: ProductRepository? = null
    private var cartRepo: CartRepository? = null
    private var favoriteRepo: FavoriteRepository? = null
    private var orderRepo: OrderRepository? = null

    private fun getDb(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context)
            database = db
            db
        }
    }

    /**
     * D9.3 — Returns whichever [RemoteDatabaseService] the
     * `BuildConfig.USE_FIREBASE` flag selects:
     *
     *  - `false` (default for debug): the in-memory
     *    [FirebaseDatabaseServiceImpl]. Lets the app run on a fresh
     *    clone without a Firebase project; the admin "Toggle
     *    connectivity" UI uses this impl's
     *    [NetworkSimulation.setOnline] hook.
     *  - `true` (release, or debug with a dev `google-services.json`):
     *    [FirestoreRemoteDatabaseService]. Currently a stub — see the
     *    class doc for what's pending.
     *
     * The return type is the [RemoteDatabaseService] contract, not the
     * concrete class, so repositories don't care which impl is active.
     */
    fun getRemoteService(context: Context): RemoteDatabaseService {
        return remoteService ?: synchronized(this) {
            val service: RemoteDatabaseService = if (BuildConfig.USE_FIREBASE) {
                FirestoreRemoteDatabaseService(context.applicationContext)
            } else {
                FirebaseDatabaseServiceImpl(context.applicationContext)
            }
            remoteService = service
            service
        }
    }

    /**
     * D9.3 — Returns the [NetworkSimulation] hook for the active
     * backend, or `null` if the backend doesn't support simulation
     * (i.e. Firestore). The admin UI / [OrderViewModel] use this to
     * expose the "Toggle connectivity" demo action; when it's `null`
     * the action is a no-op (the real backend's online state is
     * driven by `ConnectivityManager` + actual Firestore calls).
     */
    fun getNetworkSimulation(context: Context): NetworkSimulation? {
        val active = getRemoteService(context)
        return active as? NetworkSimulation
    }

    fun getProductRepository(context: Context): ProductRepository {
        return productRepo ?: synchronized(this) {
            val repo = ProductRepository(
                productDao = getDb(context).productDao(),
                remoteService = getRemoteService(context)
            )
            productRepo = repo
            repo
        }
    }

    fun getCartRepository(context: Context): CartRepository {
        return cartRepo ?: synchronized(this) {
            val repo = CartRepository(
                cartDao = getDb(context).cartDao(),
                productDao = getDb(context).productDao()
            )
            cartRepo = repo
            repo
        }
    }

    fun getFavoriteRepository(context: Context): FavoriteRepository {
        return favoriteRepo ?: synchronized(this) {
            val repo = FavoriteRepository(
                favoriteDao = getDb(context).favoriteDao()
            )
            favoriteRepo = repo
            repo
        }
    }

    fun getOrderRepository(context: Context): OrderRepository {
        return orderRepo ?: synchronized(this) {
            val repo = OrderRepository(
                orderDao = getDb(context).orderDao(),
                orderItemDao = getDb(context).orderItemDao(),
                cartDao = getDb(context).cartDao(),
                productDao = getDb(context).productDao(),
                remoteService = getRemoteService(context),
                // H3 / Phase 7B-2 — pass the database so
                // `placeCODOrder` can run its stock check + order
                // insert + stock-decrement inside a single
                // `withTransaction` for race-free behavior.
                database = getDb(context)
            )
            orderRepo = repo
            repo
        }
    }
}
