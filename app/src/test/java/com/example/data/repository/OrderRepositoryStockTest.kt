package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.data.remote.RemoteDatabaseService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * H3 / Phase 7B-2 / T5.12 — Locks the
 * [OrderRepository.placeCODOrder] stock-decrement contract.
 *
 * Three cases pin the behavior so a future regression to
 * the pre-H3 "decrement happens, no rollback" path will fail
 * loudly:
 *
 * 1. `placeOrder_decrementsStockAtomically` — happy path.
 *    Seed two products, place an order, assert both stocks
 *    went down by the right amount, assert the order + items
 *    are in the DB.
 *
 * 2. `placeOrder_throwsOutOfStock_whenItemExceedsStock` —
 *    failure path. Seed a product with `stock = 1`, try to
 *    place an order with `quantity = 2`, assert
 *    [OutOfStockException] is thrown (and re-thrown as a
 *    Result.failure to the caller).
 *
 * 3. `placeOrder_rollsBackPartialDecrement_onStockFailure` —
 *    the headline H3 contract. Seed two products, call
 *    `placeCODOrder` with a cart that overflows the second
 *    product's stock. Assert:
 *    - the call throws
 *    - the first product's stock is UNCHANGED (the
 *      `withTransaction` rolled the whole thing back, instead
 *      of leaking a partial decrement)
 *    - the order is NOT in the DB
 *    - the cart is NOT cleared
 *
 * The remote service is the default Firebase stub
 * ([com.example.data.remote.firebase.FirebaseDatabaseServiceImpl])
 * which always reports offline — that means the order is
 * written with `isSynced = false` and `Result.success(order)`
 * is returned. The remote-upload branch is already covered
 * by the H1 tests; this test stays focused on the stock
 * guard.
 *
 * ENV-BLOCKED: `./gradlew test` is not runnable on this
 * machine (no Java/Gradle wrapper). Tests are written to
 * contract; CI / a developer's machine runs them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrderRepositoryStockTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: OrderRepository
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val remoteService: RemoteDatabaseService = com.example.data.di.ServiceLocator
            .getRemoteService(context)
        repo = OrderRepository(
            orderDao = db.orderDao(),
            orderItemDao = db.orderItemDao(),
            cartDao = db.cartDao(),
            productDao = db.productDao(),
            remoteService = remoteService,
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun placeOrder_decrementsStockAtomically() = runTest {
        db.productDao().insertProducts(
            listOf(
                product("p_alpha", stock = 10),
                product("p_beta", stock = 5)
            )
        )

        val items = listOf(
            OrderItem(orderId = "", productId = "p_alpha", nameAr = "ألفا", nameEn = "Alpha", price = 1.0, quantity = 3),
            OrderItem(orderId = "", productId = "p_beta", nameAr = "بيتا", nameEn = "Beta", price = 1.0, quantity = 2)
        )

        val result = repo.placeCODOrder(
            customerName = "Ahmad",
            customerAddress = "Baghdad",
            customerPhone = "+9647700000000",
            totalPrice = 5.0,
            items = items
        )

        assertTrue("placeCODOrder should succeed", result.isSuccess)
        assertEquals("p_alpha stock should be 10 - 3 = 7", 7, db.productDao().getStockDirect("p_alpha"))
        assertEquals("p_beta stock should be 5 - 2 = 3", 3, db.productDao().getStockDirect("p_beta"))
    }

    @Test
    fun placeOrder_throwsOutOfStock_whenItemExceedsStock() = runTest {
        db.productDao().insertProducts(listOf(product("p_alpha", stock = 1)))

        val items = listOf(
            OrderItem(orderId = "", productId = "p_alpha", nameAr = "ألفا", nameEn = "Alpha", price = 1.0, quantity = 2)
        )

        val result = repo.placeCODOrder(
            customerName = "Ahmad",
            customerAddress = "Baghdad",
            customerPhone = "+9647700000000",
            totalPrice = 2.0,
            items = items
        )

        assertTrue("placeCODOrder should fail", result.isFailure)
        val failure = result.exceptionOrNull()
        assertNotNull("exception should be present", failure)
        assertTrue(
            "failure should be OutOfStockException, was ${failure!!::class.simpleName}",
            failure is OutOfStockException
        )
        val oos = failure as OutOfStockException
        assertEquals("p_alpha", oos.productId)
        assertEquals(1, oos.availableStock)
        assertEquals(2, oos.requestedQuantity)
        assertEquals(
            "stock should not have been decremented when the order fails",
            1,
            db.productDao().getStockDirect("p_alpha")
        )
    }

    @Test
    fun placeOrder_rollsBackPartialDecrement_onStockFailure() = runTest {
        db.productDao().insertProducts(
            listOf(
                product("p_alpha", stock = 10),
                product("p_beta", stock = 1)
            )
        )

        val items = listOf(
            OrderItem(orderId = "", productId = "p_alpha", nameAr = "ألفا", nameEn = "Alpha", price = 1.0, quantity = 3),
            OrderItem(orderId = "", productId = "p_beta", nameAr = "بيتا", nameEn = "Beta", price = 1.0, quantity = 2)
        )

        val result = repo.placeCODOrder(
            customerName = "Ahmad",
            customerAddress = "Baghdad",
            customerPhone = "+9647700000000",
            totalPrice = 5.0,
            items = items
        )

        assertTrue("placeCODOrder should fail when the second item is OOS", result.isFailure)
        // The critical H3 assertion: p_alpha's stock must NOT have
        // been decremented, even though p_alpha was the *first*
        // item and its decrement would have succeeded in
        // isolation. The `withTransaction` must roll back the
        // whole sequence.
        assertEquals(
            "p_alpha stock should be untouched — the transaction must roll back",
            10,
            db.productDao().getStockDirect("p_alpha")
        )
        assertEquals(
            "p_beta stock should be untouched — the transaction must roll back",
            1,
            db.productDao().getStockDirect("p_beta")
        )
        // The order must not be in the DB.
        val allOrders = db.orderDao().getUnsyncedOrdersWithItems()
        assertEquals("no order should be persisted when the stock check fails", 0, allOrders.size)
    }

    private fun product(id: String, stock: Int) = Product(
        id = id,
        nameAr = "منتج $id",
        nameEn = "Product $id",
        descriptionAr = "وصف $id",
        descriptionEn = "Desc $id",
        imageUrl = "https://example.com/$id.jpg",
        price = 1.0,
        categoryAr = "فئة",
        categoryEn = "Category",
        stock = stock
    )
}
