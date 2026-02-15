package org.liftrr.data.local

import androidx.room.TypeConverter
import org.liftrr.data.models.*

/**
 * Room type converters for custom types and enums
 */
class Converters {

    // SyncStatus converters
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncStatus.PENDING
        }
    }

    // SyncOperation converters
    @TypeConverter
    fun fromSyncOperation(value: SyncOperation): String {
        return value.name
    }

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation {
        return try {
            SyncOperation.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncOperation.INSERT
        }
    }

    // AuthProvider converters
    @TypeConverter
    fun fromAuthProvider(value: AuthProvider): String {
        return value.name
    }

    @TypeConverter
    fun toAuthProvider(value: String): AuthProvider {
        return try {
            AuthProvider.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AuthProvider.EMAIL_PASSWORD
        }
    }

    // Gender converters
    @TypeConverter
    fun fromGender(value: Gender?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGender(value: String?): Gender? {
        return value?.let {
            try {
                Gender.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // FitnessLevel converters
    @TypeConverter
    fun fromFitnessLevel(value: FitnessLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFitnessLevel(value: String?): FitnessLevel? {
        return value?.let {
            try {
                FitnessLevel.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // UnitSystem converters
    @TypeConverter
    fun fromUnitSystem(value: UnitSystem): String {
        return value.name
    }

    @TypeConverter
    fun toUnitSystem(value: String): UnitSystem {
        return try {
            UnitSystem.valueOf(value)
        } catch (e: IllegalArgumentException) {
            UnitSystem.METRIC
        }
    }

    // PromptType converters
    @TypeConverter
    fun fromPromptType(value: PromptType): String {
        return value.name
    }

    @TypeConverter
    fun toPromptType(value: String): PromptType {
        return try {
            PromptType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PromptType.FITNESS_LEVEL
        }
    }
}
