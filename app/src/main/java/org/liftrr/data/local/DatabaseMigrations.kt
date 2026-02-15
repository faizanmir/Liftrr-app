package org.liftrr.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN deletedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_isDeleted ON workout_sessions(isDeleted)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN keyFramesJson TEXT")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add sync fields to workout_sessions table
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN lastSyncedAt INTEGER")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN videoCloudUrl TEXT")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN keyFramesCloudUrls TEXT")

            // Add indices for workout_sessions
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_userId ON workout_sessions(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_syncStatus ON workout_sessions(syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_userId_timestamp ON workout_sessions(userId, timestamp)")

            // Add profile fields to users table
            db.execSQL("ALTER TABLE users ADD COLUMN photoCloudUrl TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN dateOfBirth INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN height REAL")
            db.execSQL("ALTER TABLE users ADD COLUMN weight REAL")
            db.execSQL("ALTER TABLE users ADD COLUMN fitnessLevel TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN goalsJson TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN preferredExercises TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN preferredUnits TEXT NOT NULL DEFAULT 'METRIC'")
            db.execSQL("ALTER TABLE users ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE users ADD COLUMN reminderTime TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            db.execSQL("ALTER TABLE users ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE users ADD COLUMN lastSyncedAt INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

            // Add indices for users table
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_users_syncStatus ON users(syncStatus)")

            // Create sync_queue table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    data TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    lastAttemptAt INTEGER,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    maxRetries INTEGER NOT NULL DEFAULT 5,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    lastError TEXT,
                    errorDetails TEXT
                )
            """.trimIndent())

            // Add indices for sync_queue table
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType ON sync_queue(entityType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_syncStatus ON sync_queue(syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdAt ON sync_queue(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType_syncStatus ON sync_queue(entityType, syncStatus)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create user_prompts table for progressive profiling
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS user_prompts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    promptType TEXT NOT NULL,
                    firstShownAt INTEGER NOT NULL,
                    lastShownAt INTEGER NOT NULL,
                    timesShown INTEGER NOT NULL DEFAULT 1,
                    completed INTEGER NOT NULL DEFAULT 0,
                    completedAt INTEGER,
                    dismissed INTEGER NOT NULL DEFAULT 0,
                    dismissedAt INTEGER,
                    shouldShowAgain INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            // Add indices for user_prompts table
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_prompts_userId ON user_prompts(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_prompts_promptType ON user_prompts(promptType)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_prompts_userId_promptType ON user_prompts(userId, promptType)")
        }
    }

    /**
     * List of all migrations in order
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
    )
}
