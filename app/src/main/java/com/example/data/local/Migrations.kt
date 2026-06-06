package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * H2 / Phase 7B-2 — Real Room migration for v2 → v3.
 *
 * Adds the `archivedAt INTEGER` column to the `products` table.
 * The column is nullable by definition in SQLite, so existing
 * rows (which had no concept of archive) just get `null` =
 * "active product", preserving their visibility in the
 * storefront queries (`getAllProductsFlow` etc. all filter by
 * `WHERE archivedAt IS NULL`).
 *
 * T4.5 / T4.6 — The actual `ALTER TABLE` lives here (not in
 * `RoomComponents.kt`) so the migration list stays a single
 * auditable place. The Robolectric `MigrationTest.kt` boots
 * Room at version 2, seeds rows, then runs this migration and
 * asserts the rows come back with `archivedAt = null`.
 *
 * The destructive fallback in `AppDatabase.getDatabase` is
 * kept as a safety net for *v0* and *v1* users (whose schemas
 * we never had a migration path for). Users on v2 get the
 * real migration, so order history is preserved.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN archivedAt INTEGER")
    }
}
