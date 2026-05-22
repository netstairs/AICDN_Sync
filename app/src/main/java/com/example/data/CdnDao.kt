package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CdnDao {
    // Edge Nodes Querying
    @Query("SELECT * FROM cdn_edge_nodes ORDER BY id ASC")
    fun getAllEdgeNodes(): Flow<List<CdnEdgeNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdgeNode(node: CdnEdgeNode)

    @Update
    suspend fun updateEdgeNode(node: CdnEdgeNode)

    @Query("UPDATE cdn_edge_nodes SET activeLoad = :load, bandwidthMbps = :bandwidth, cacheHitRate = :hitRate WHERE id = :id")
    suspend fun updateNodeMetrics(id: Int, load: Int, bandwidth: Int, hitRate: Float)

    @Query("UPDATE cdn_edge_nodes SET status = :status WHERE id = :id")
    suspend fun updateNodeStatus(id: Int, status: String)

    // Content Handling
    @Query("SELECT * FROM content_items ORDER BY creationTime DESC")
    fun getAllContentItems(): Flow<List<ContentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItem(item: ContentItem): Long

    @Update
    suspend fun updateContentItem(item: ContentItem)

    @Query("UPDATE content_items SET syncStatus = :status, progress = :progress WHERE id = :id")
    suspend fun updateContentProgress(id: Int, status: String, progress: Int)

    @Query("DELETE FROM content_items WHERE id = :id")
    suspend fun deleteContentItem(id: Int)

    // Real-Time Activity Monitoring
    @Query("SELECT * FROM sync_activity_records ORDER BY timestamp DESC LIMIT 40")
    fun getAllSyncActivityRecords(): Flow<List<SyncActivityRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncActivityRecord(record: SyncActivityRecord)

    @Query("DELETE FROM sync_activity_records")
    suspend fun clearSyncActivityRecords()

    // AI Policy Definitions
    @Query("SELECT * FROM ai_optimization_rules ORDER BY confidenceScore DESC")
    fun getAllOptimizationRules(): Flow<List<AiOptimizationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationRule(rule: AiOptimizationRule)

    @Update
    suspend fun updateOptimizationRule(rule: AiOptimizationRule)

    @Query("UPDATE ai_optimization_rules SET isActive = :isActive WHERE id = :id")
    suspend fun setRuleActiveState(id: Int, isActive: Boolean)
}
