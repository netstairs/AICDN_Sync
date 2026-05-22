package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_activity_records")
data class SyncActivityRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contentItemName: String,
    val platform: String,
    val speedMbps: Float,
    val progress: Int,
    val status: String, // "COMPLETED", "ACTIVE", "ERROR"
    val timestamp: Long = System.currentTimeMillis()
)
