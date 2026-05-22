package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CdnViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CdnDatabase.getDatabase(application, viewModelScope)
    private val repository = CdnRepository(database.cdnDao())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentNodes = repository.edgeNodes.first()
                if (currentNodes.isEmpty()) {
                    val defaultNodes = listOf(
                        CdnEdgeNode(name = "Edge Tokyo-North", region = "Asia-Pacific (Tokyo)", ipAddress = "192.16.8.10", status = "ONLINE", activeLoad = 45, bandwidthMbps = 720, cacheHitRate = 0.94f),
                        CdnEdgeNode(name = "Edge London-West", region = "Europe (London)", ipAddress = "192.16.4.15", status = "ONLINE", activeLoad = 62, bandwidthMbps = 850, cacheHitRate = 0.91f),
                        CdnEdgeNode(name = "Edge Frankfurt-Main", region = "Europe (Frankfurt)", ipAddress = "192.16.5.12", status = "ONLINE", activeLoad = 24, bandwidthMbps = 940, cacheHitRate = 0.96f),
                        CdnEdgeNode(name = "Edge Oregon-Valley", region = "US-West (Oregon)", ipAddress = "192.16.1.20", status = "ONLINE", activeLoad = 78, bandwidthMbps = 1100, cacheHitRate = 0.89f),
                        CdnEdgeNode(name = "Edge Sydney-Harbour", region = "Asia-Pacific (Sydney)", ipAddress = "192.16.9.8", status = "ONLINE", activeLoad = 15, bandwidthMbps = 410, cacheHitRate = 0.98f),
                        CdnEdgeNode(name = "Edge São Paulo", region = "South America (São Paulo)", ipAddress = "192.16.12.3", status = "DEGRADED", activeLoad = 89, bandwidthMbps = 280, cacheHitRate = 0.72f)
                    )
                    for (node in defaultNodes) {
                        repository.insertEdgeNode(node)
                    }
                }

                val currentRules = repository.optimizationRules.first()
                if (currentRules.isEmpty()) {
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
                        repository.insertRule(rule)
                    }
                }

                val currentContent = repository.contentItems.first()
                if (currentContent.isEmpty()) {
                    val defaultItem = ContentItem(
                        name = "Global_Ad_Campaign_2026_FHD.mp4",
                        mimeType = "Video (MP4)",
                        sizeKb = 48500,
                        sourceUrl = "https://cdn.example.com/assets/video_fhd.mp4",
                        targetPlatforms = "YouTube, Twitch, AWS S3",
                        syncStatus = "COMPLETED",
                        progress = 100
                    )
                    repository.insertContentItem(defaultItem)

                    val defaultRecords = listOf(
                        SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "YouTube", speedMbps = 450f, progress = 100, status = "COMPLETED"),
                        SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "Twitch", speedMbps = 250f, progress = 100, status = "COMPLETED"),
                        SyncActivityRecord(contentItemName = "Global_Ad_Campaign_2026_FHD.mp4", platform = "AWS S3", speedMbps = 900f, progress = 100, status = "COMPLETED")
                    )
                    for (record in defaultRecords) {
                        repository.insertSyncRecord(record)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CdnViewModel", "Dynamic startup database loading failed", e)
            }
        }
    }

    // Flows for reactive UI updates from Room
    val edgeNodes = repository.edgeNodes
    val contentItems = repository.contentItems
    val syncRecords = repository.syncRecords
    val optimizationRules = repository.optimizationRules

    // AI Advice State
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Notification and snackbar broadcast messages
    private val _syncingState = MutableStateFlow<String?>(null)
    val syncingState: StateFlow<String?> = _syncingState.asStateFlow()

    fun resetSyncingState() {
        _syncingState.value = null
    }

    fun simulateReplication(name: String, mimeType: String, sizeKb: Int, platforms: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetStr = platforms.joinToString(", ")
            val item = ContentItem(
                name = name,
                mimeType = mimeType,
                sizeKb = sizeKb,
                sourceUrl = "https://origin.nexuscdn.net/assets/${name.replace(" ", "_")}",
                targetPlatforms = targetStr,
                syncStatus = "ACTIVE",
                progress = 10
            )
            val itemId = repository.insertContentItem(item)

            _syncingState.value = "Starting CDN multi-platform broadcast for $name"

            repository.insertSyncRecord(
                SyncActivityRecord(
                    contentItemName = name,
                    platform = "Origin Prep",
                    speedMbps = 1500f,
                    progress = 10,
                    status = "ACTIVE"
                )
            )

            val progressSteps = listOf(30, 65, 85, 100)
            for (p in progressSteps) {
                delay(1200)
                val status = if (p == 100) "COMPLETED" else "ACTIVE"
                repository.updateContentProgress(itemId.toInt(), status, p)

                for (platform in platforms) {
                    val simulatedSpeed = (350..1150).random().toFloat()
                    val recordProgress = if (p == 100) 100 else (p + (-10..10).random()).coerceIn(10, 99)
                    repository.insertSyncRecord(
                        SyncActivityRecord(
                            contentItemName = name,
                            platform = platform,
                            speedMbps = simulatedSpeed,
                            progress = recordProgress,
                            status = if (recordProgress == 100) "COMPLETED" else "ACTIVE"
                        )
                    )
                }
            }
            _syncingState.value = "CDN successfully synced '$name' to edge caches."
        }
    }

    fun flushNodeCache(node: CdnEdgeNode) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncingState.value = "Broadcasting cache flush on node [${node.name}]"
            repository.updateNodeStatus(node.id, "DEGRADED")
            repository.updateNodeMetrics(node.id, 2, 0, 0.00f)
            delay(1500)
            repository.updateNodeStatus(node.id, "ONLINE")
            repository.updateNodeMetrics(node.id, 8, 1400, 0.99f)
            _syncingState.value = "Pristine edge replica created for [${node.name}]."
        }
    }

    fun simulateEdgeBurst(node: CdnEdgeNode) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncingState.value = "Simulating load-test spike on region: [${node.name}]"
            // Spike metrics
            repository.updateNodeMetrics(node.id, 96, 2100, 0.79f)
            repository.insertSyncRecord(
                SyncActivityRecord(
                    contentItemName = "Edge_StressTest_${(100..999).random()}",
                    platform = "Edge [${node.name.split(" ").last()}]",
                    speedMbps = 2100f,
                    progress = 100,
                    status = "COMPLETED"
                )
            )
            delay(2500)
            // Restore standard metrics
            val activeL = (20..65).random()
            val speedM = (300..950).random()
            val hitRate = (85..98).random() / 100f
            repository.updateNodeMetrics(node.id, activeL, speedM, hitRate)
            _syncingState.value = "Stress test complete on [${node.name}]. Resuming load-balancing flow."
        }
    }

    fun toggleRule(rule: AiOptimizationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRuleActiveState(rule.id, !rule.isActive)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSyncRecords()
            _syncingState.value = "System active sync logging terminal flushed."
        }
    }

    fun askGeminiAdvisor(userQuery: String) {
        val apiKey = when {
            BuildConfig.MY_GEMINI_API_KEY.isNotEmpty() && 
                BuildConfig.MY_GEMINI_API_KEY != "MY_GEMINI_API_KEY_PLACEHOLDER" && 
                BuildConfig.MY_GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.MY_GEMINI_API_KEY
            BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
                BuildConfig.GEMINI_API_KEY != "GEMINI_API_KEY_PLACEHOLDER" && 
                BuildConfig.GEMINI_API_KEY != "GEMINI_API_KEY" && 
                BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isEmpty()) {
            // Standard simulated diagnostic advisor if no key is entered
            viewModelScope.launch {
                _aiLoading.value = true
                delay(1800)
                _aiResponse.value = generateSimulatedAdvice(userQuery)
                _aiLoading.value = false
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _aiLoading.value = true
            try {
                // Compile the precise JSON-like state context
                val nodesList = edgeNodes.first().joinToString("\n") {
                    "- Node [${it.name}] / ${it.region}: Load=${it.activeLoad}%, status=${it.status}, hitRate=${(it.cacheHitRate * 100).toInt()}%"
                }
                val rulesList = optimizationRules.first().joinToString("\n") {
                    "- Policy [${it.title}] (${it.ruleType}): active=${it.isActive}, score=${it.confidenceScore}"
                }

                val systemPrompt = """
                    You are Nexus-AI CDN Advisor, an operational caching intelligence engine integrated into an enterprise CDN dashboard in Android.
                    Analyze this real-time system configuration to assist the datacenter manager's inquiry:
                    
                    GLOBAL CACHING REPLICAS:
                    $nodesList
                    
                    ACTIVE OPTIMIZATION RULES:
                    $rulesList
                    
                    Answer the user query: "$userQuery". Respond using concise, highly technical bullet points. Prioritize cache-invalidation suggestions, regional bandwidth allocation adjustments, or failovers to keep latency minimal. Ensure readability, maximum 150 words.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userQuery)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val adviceText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No recommendation generated automatically. Try rephrasing the rule triggers."
                
                _aiResponse.value = adviceText
            } catch (e: Exception) {
                _aiResponse.value = "Diagnostic Fallback Recommendation:\n\n" + generateSimulatedAdvice(userQuery)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private fun generateSimulatedAdvice(query: String): String {
        return """
            • [Hot-Cache Allocation]: Detected heavy loads on Edge Oregon-Valley (78% traffic saturation). AI engine suggests enabling dynamic routing policies to Frankfurt-Main nodes.
            
            • [Replication Failure Prevention]: The São Paulo replica is suffering degraded status (89% load, 72% Cache Hits). Automated failover to secondary AWS S3 storage is recommended immediately.
            
            • [Actionable Optimization for '$query']: Pre-warm regional APAC storage blocks with cache preconditioning before broadcast tasks. Enable lossless dynamic Compression on all image assets.
        """.trimIndent()
    }
}
