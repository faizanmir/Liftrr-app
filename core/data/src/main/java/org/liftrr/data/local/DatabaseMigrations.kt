package org.liftrr.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `users`")
            db.execSQL("DROP TABLE IF EXISTS `user_profiles`")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sync_queue` (
                    `queueId` TEXT NOT NULL,
                    `sessionId` TEXT NOT NULL,
                    `operation` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `attemptCount` INTEGER NOT NULL,
                    `lastError` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `nextEligibleAt` INTEGER NOT NULL,
                    PRIMARY KEY(`queueId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_sync_queue_sessionId` ON `workout_sync_queue` (`sessionId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_sync_queue_state_nextEligibleAt` " +
                    "ON `workout_sync_queue` (`state`, `nextEligibleAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_sync_queue_operation` ON `workout_sync_queue` (`operation`)"
            )
        }
    }

    val ALL_MIGRATIONS = arrayOf<Migration>(MIGRATION_1_2, MIGRATION_2_3)
}
