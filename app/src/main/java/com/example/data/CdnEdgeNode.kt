package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cdn_edge_nodes")
data class CdnEdgeNode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val region: String,
    val ipAddress: String,
    val status: String, // "ONLINE", "DEGRADED", "OFFLINE"
    val activeLoad: Int, // 0 - 100
    val bandwidthMbps: Int,
    val cacheHitRate: Float // 0.0f - 1.0f
)
