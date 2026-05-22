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
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
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
