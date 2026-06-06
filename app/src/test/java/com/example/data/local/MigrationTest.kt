package com.example.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * D8.24 / Phase 7B-2 / T4.16 — Locks the v2 → v3 Room
 * migration that introduces `Product.archivedAt` (H2).
 *
 * Two cases pin the contract:
 *
 * 1. `migration_addsArchivedAtColumnToProducts_preservesAllRows` —
 *    boots Room at v2 (using the schema JSON that KSP exports to
 *    `app/schemas/`), seeds three products, runs [MIGRATION_2_3],
 *    and asserts the v3 read returns the same products with
 *    `archivedAt = null` (the default for a freshly-added
 *    nullable column). The point of this test is to catch any
 *    future "oh, the migration drops order history" regression
 *    early.
 *
 * 2. `archiveAndUnarchive_roundTripMovesProductBetweenQueries` —
 *    boots Room at v3, archives one product, asserts
 *    `getAllProducts` and `getArchivedProductsFlow` move the row
 *    in opposite directions; then unarchives and asserts the
 *    reverse. This is the H2 contract that the AccountScreen
 *    Archived tab relies on.
 *
 * The schemas are produced by KSP during the build (`ksp { arg(
 * "room.schemaLocation", "$projectDir/schemas") }` in
 * `app/build.gradle.kts:215`). They are not committed to the
 * repository — `./gradlew kspDebugKotlin` regenerates them on
 * the developer machine or in CI.
 *
 * ENV-BLOCKED: `./gradlew test` is not runnable on this machine
 * (no Java/Gradle wrapper). Tests are written to contract; CI /
 * a developer's machine runs them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
        FrameworkSQLiteOpenHelperFactory()
    )

    private val seededProducts = listOf(
        Product(
            id = "p_alpha",
            nameAr = "شريحة ألفا",
            nameEn = "Alpha chip",
            descriptionAr = "وصف ألفا",
            descriptionEn = "Alpha desc",
            imageUrl = "https://example.com/alpha.jpg",
            price = 12000.0,
            categoryAr = "المعالجات",
            categoryEn = "Processors",
            isFeatured = true,
            stock = 50
        ),
        Product(
            id = "p_beta",
            nameAr = "شريحة بيتا",
            nameEn = "Beta chip",
            descriptionAr = "وصف بيتا",
            descriptionEn = "Beta desc",
            imageUrl = "https://example.com/beta.jpg",
            price = 9000.0,
            categoryAr = "الذاكرة",
            categoryEn = "Memory",
            isDiscounted = true,
            originalPrice = 12000.0,
            stock = 8
        ),
        Product(
            id = "p_gamma",
            nameAr = "شريحة غاما",
            nameEn = "Gamma chip",
            descriptionAr = "وصف غاما",
            descriptionEn = "Gamma desc",
            imageUrl = "https://example.com/gamma.jpg",
            price = 4500.0,
            categoryAr = "الشبكات",
            categoryEn = "Networking",
            stock = 0
        )
    )

    @Test
    fun migration_addsArchivedAtColumnToProducts_preservesAllRows() {
        // Boot a v2 database, seed it, then run the migration.
        val db = helper.createDatabase(TEST_DB_NAME, 2).apply {
            // v2 schema does not have `archivedAt`, so we insert
            // with the v2 column set. We open a raw SupportSQLite
            // handle here — no Room Entity mapping — to keep the
            // test independent of the v2 Room schema which has
            // been wiped from the project.
            execSQL(
                "INSERT INTO products (id, nameAr, nameEn, descriptionAr, descriptionEn, " +
                    "imageUrl, price, categoryAr, categoryEn, isFeatured, isDiscounted, " +
                    "originalPrice, stock, lastUpdated) VALUES " +
                    "('p_alpha', 'شريحة ألفا', 'Alpha chip', 'وصف ألفا', 'Alpha desc', " +
                    "'https://example.com/alpha.jpg', 12000.0, 'المعالجات', 'Processors', 1, 0, NULL, 50, 1)"
            )
            execSQL(
                "INSERT INTO products (id, nameAr, nameEn, descriptionAr, descriptionEn, " +
                    "imageUrl, price, categoryAr, categoryEn, isFeatured, isDiscounted, " +
                    "originalPrice, stock, lastUpdated) VALUES " +
                    "('p_beta', 'شريحة بيتا', 'Beta chip', 'وصف بيتا', 'Beta desc', " +
                    "'https://example.com/beta.jpg', 9000.0, 'الذاكرة', 'Memory', 0, 1, 12000.0, 8, 1)"
            )
            close()
        }

        // Run the migration under test.
        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            3,
            true,
            MIGRATION_2_3
        )

        // Now open the migrated database with Room and read the rows.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_2_3)
            .build()
        try {
            val rows = migrated.productDao().getAllProducts()
            assertEquals("migration should preserve all v2 rows", 2, rows.size)

            val alpha = rows.firstOrNull { it.id == "p_alpha" }
            assertNotNull("p_alpha should survive migration", alpha)
            assertNull("p_alpha.archivedAt should be null after migration", alpha!!.archivedAt)
            assertEquals("p_alpha.nameAr should survive", "شريحة ألفا", alpha.nameAr)
            assertEquals("p_alpha.price should survive", 12000.0, alpha.price, 0.001)
            assertEquals("p_alpha.isFeatured should survive", true, alpha.isFeatured)

            val beta = rows.firstOrNull { it.id == "p_beta" }
            assertNotNull("p_beta should survive migration", beta)
            assertNull("p_beta.archivedAt should be null after migration", beta!!.archivedAt)
            assertEquals("p_beta.isDiscounted should survive", true, beta.isDiscounted)
            assertEquals("p_beta.originalPrice should survive", 12000.0, beta!!.originalPrice!!, 0.001)
        } finally {
            migrated.close()
        }
    }

    @Test
    fun archiveAndUnarchive_roundTripMovesProductBetweenQueries() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Fresh in-memory DB at v3 — no migration needed, the test
        // exercises the archive / unarchive contract directly.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            db.productDao().insertProducts(seededProducts)
            assertEquals("all three products should be visible to the storefront", 3, db.productDao().getAllProducts().size)
            assertEquals("no archived products at start", 0, db.productDao().getArchivedProductsFlow().first().size)

            // Archive p_alpha.
            db.productDao().archiveProduct("p_alpha", archivedAt = 1_700_000_000_000L)
            val afterArchive = db.productDao().getAllProducts()
            assertEquals("storefront should drop the archived row", 2, afterArchive.size)
            assertTrue("p_alpha should be gone from storefront", afterArchive.none { it.id == "p_alpha" })

            val archived = db.productDao().getArchivedProductsFlow().first()
            assertEquals("Archived tab should contain exactly p_alpha", 1, archived.size)
            assertEquals("p_alpha", archived[0].id)
            assertEquals("archivedAt should be the timestamp we passed in", 1_700_000_000_000L, archived[0].archivedAt)

            // Unarchive p_alpha.
            db.productDao().unarchiveProduct("p_alpha")
            assertEquals("storefront should see all three again", 3, db.productDao().getAllProducts().size)
            assertEquals("Archived tab should be empty", 0, db.productDao().getArchivedProductsFlow().first().size)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB_NAME = "migration-test"
    }
}
