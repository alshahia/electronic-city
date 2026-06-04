package com.example.data.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.remote.firebase.FirebaseDatabaseServiceImpl
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
    private var remoteService: FirebaseDatabaseServiceImpl? = null

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

    fun getRemoteService(context: Context): FirebaseDatabaseServiceImpl {
        return remoteService ?: synchronized(this) {
            val service = FirebaseDatabaseServiceImpl(context.applicationContext)
            remoteService = service
            service
        }
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
                remoteService = getRemoteService(context)
            )
            orderRepo = repo
            repo
        }
    }
}
