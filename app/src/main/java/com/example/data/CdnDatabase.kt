package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CdnEdgeNode::class,
        ContentItem::class,
        SyncActivityRecord::class,
        AiOptimizationRule::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CdnDatabase : RoomDatabase() {
    abstract fun cdnDao(): CdnDao

    companion object {
        @Volatile
        private var INSTANCE: CdnDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CdnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CdnDatabase::class.java,
                    "cdn_database"
                )
                .addCallback(CdnDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class CdnDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.cdnDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: CdnDao) {
                // Populate default global nodes
                val defaultNodes = listOf(
                    CdnEdgeNode(name = "Edge Tokyo-North", region = "Asia-Pacific (Tokyo)", ipAddress = "192.16.8.10", status = "ONLINE", activeLoad = 45, bandwidthMbps = 720, cacheHitRate = 0.94f),
                    CdnEdgeNode(name = "Edge London-West", region = "Europe (London)", ipAddress = "192.16.4.15", status = "ONLINE", activeLoad = 62, bandwidthMbps = 850, cacheHitRate = 0.91f),
                    CdnEdgeNode(name = "Edge Frankfurt-Main", region = "Europe (Frankfurt)", ipAddress = "192.16.5.12", status = "ONLINE", activeLoad = 24, bandwidthMbps = 940, cacheHitRate = 0.96f),
                    CdnEdgeNode(name = "Edge Oregon-Valley", region = "US-West (Oregon)", ipAddress = "192.16.1.20", status = "ONLINE", activeLoad = 78, bandwidthMbps = 1100, cacheHitRate = 0.89f),
                    CdnEdgeNode(name = "Edge Sydney-Harbour", region = "Asia-Pacific (Sydney)", ipAddress = "192.16.9.8", status = "ONLINE", activeLoad = 15, bandwidthMbps = 410, cacheHitRate = 0.98f),
                    CdnEdgeNode(name = "Edge São Paulo", region = "South America (São Paulo)", ipAddress = "192.16.12.3", status = "DEGRADED", activeLoad = 89, bandwidthMbps = 280, cacheHitRate = 0.72f)
                )
                for (node in defaultNodes) {
                    dao.insertEdgeNode(node)
                }

                // Populate initial AI optimization recommendations
                val defaultRules = listOf(
                    AiOptimizationRule(
                        title = "Predictive Cache Pre-warming",
                        description = "Warm up European edge-caches with video media payloads before regional peak viewing slots (6:00 PM - 11:00 PM CET).",
                        ruleType = "Pre-warming",
                        isActive = true,
                        confidenceScore = 0.95f,
                        estimatedSavingsGb = 1450f
                    ),
                    AiOptimizationRule(
                        title = "Adaptive WebP/AVIF Compression",
                        description = "Enable on-the-fly graphic asset conversion for users on high-latency mobile configurations.",
                        ruleType = "Edge Compression",
                        isActive = false,
                        confidenceScore = 0.88f,
                        estimatedSavingsGb = 620f
                    ),
                    AiOptimizationRule(
                        title = "Latency-Balanced failover",
                        description = "Reroute South American transit automatically through US Oregon-Valley relays if Frankfurt delays cross 250ms.",
                        ruleType = "Intelligent Routing",
                        isActive = true,
                        confidenceScore = 0.92f,
                        estimatedSavingsGb = 810f
                    )
                )
                for (rule in defaultRules) {
                    dao.insertOptimizationRule(rule)
                }

                // Initial upload content placeholder
                val item = ContentItem(
                    name = "Global_Ad_Campaign_2026_FHD.mp4",
                    mimeType = "Video (MP4)",
                    sizeKb = 48500,
                    sourceUrl = "https://cdn.example.com/assets/video_fhd.mp4",
                    targetPlatforms = "YouTube, Twitch, AWS S3",
                    syncStatus = "COMPLETED",
                    progress = 100
                )
                dao.insertContentItem(item)

                val records = listOf(
                    SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "YouTube", speedMbps = 450f, progress = 100, status = "COMPLETED"),
                    SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "Twitch", speedMbps = 250f, progress = 100, status = "COMPLETED"),
                    SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "AWS S3", speedMbps = 900f, progress = 100, status = "COMPLETED")
                )
                for (record in records) {
                    dao.insertSyncActivityRecord(record)
                }
            }
        }
    }
}
