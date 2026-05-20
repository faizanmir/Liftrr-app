package org.liftrr.data.local

import androidx.room.TypeConverter
import org.liftrr.data.models.dto.SyncStatus

class Converters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        try { SyncStatus.valueOf(value) } catch (e: IllegalArgumentException) { SyncStatus.PENDING }
}
