package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_optimization_rules")
data class AiOptimizationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val ruleType: String, // "Pre-warming", "Edge Compression", "Intelligent Routing"
    val isActive: Boolean,
    val confidenceScore: Float, // 0.0 - 1.0
    val estimatedSavingsGb: Float
)
