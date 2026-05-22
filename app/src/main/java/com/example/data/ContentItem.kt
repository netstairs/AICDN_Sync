package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_items")
data class ContentItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mimeType: String, // "Video (MP4)", "Image (JPEG)", "JSON Manifest", etc.
    val sizeKb: Int,
    val sourceUrl: String,
    val targetPlatforms: String, // comma separated, e.g. "YouTube, Twitch, Vimeo, AWS S3"
    val syncStatus: String, // "PENDING", "ACTIVE", "COMPLETED", "FAILED"
    val progress: Int, // 0 - 100
    val creationTime: Long = System.currentTimeMillis()
)
