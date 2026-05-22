package com.example.data

import kotlinx.coroutines.flow.Flow

class CdnRepository(private val cdnDao: CdnDao) {
    // Live Reactive Streams
    val edgeNodes: Flow<List<CdnEdgeNode>> = cdnDao.getAllEdgeNodes()
    val contentItems: Flow<List<ContentItem>> = cdnDao.getAllContentItems()
    val syncRecords: Flow<List<SyncActivityRecord>> = cdnDao.getAllSyncActivityRecords()
    val optimizationRules: Flow<List<AiOptimizationRule>> = cdnDao.getAllOptimizationRules()

    // Node updates
    suspend fun insertEdgeNode(node: CdnEdgeNode) {
        cdnDao.insertEdgeNode(node)
    }

    suspend fun updateEdgeNode(node: CdnEdgeNode) {
        cdnDao.updateEdgeNode(node)
    }

    suspend fun updateNodeMetrics(id: Int, load: Int, bandwidth: Int, hitRate: Float) {
        cdnDao.updateNodeMetrics(id, load, bandwidth, hitRate)
    }

    suspend fun updateNodeStatus(id: Int, status: String) {
        cdnDao.updateNodeStatus(id, status)
    }

    // File records and synchronization workflows
    suspend fun insertContentItem(item: ContentItem): Long {
        return cdnDao.insertContentItem(item)
    }

    suspend fun updateContentItem(item: ContentItem) {
        cdnDao.updateContentItem(item)
    }

    suspend fun updateContentProgress(id: Int, status: String, progress: Int) {
        cdnDao.updateContentProgress(id, status, progress)
    }

    suspend fun deleteContentItem(id: Int) {
        cdnDao.deleteContentItem(id)
    }

    // Sync records logs
    suspend fun insertSyncRecord(record: SyncActivityRecord) {
        cdnDao.insertSyncActivityRecord(record)
    }

    suspend fun clearSyncRecords() {
        cdnDao.clearSyncActivityRecords()
    }

    // Rules management
    suspend fun insertRule(rule: AiOptimizationRule) {
        cdnDao.insertOptimizationRule(rule)
    }

    suspend fun updateRule(rule: AiOptimizationRule) {
        cdnDao.updateOptimizationRule(rule)
    }

    suspend fun setRuleActiveState(id: Int, isActive: Boolean) {
        cdnDao.setRuleActiveState(id, isActive)
    }
}
