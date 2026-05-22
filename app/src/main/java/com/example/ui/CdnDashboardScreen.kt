package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CdnDashboardScreen(viewModel: CdnViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Observe local Room stateflows
    val nodes by viewModel.edgeNodes.collectAsStateWithLifecycle(initialValue = emptyList())
    val contentItems by viewModel.contentItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val syncRecords by viewModel.syncRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val rules by viewModel.optimizationRules.collectAsStateWithLifecycle(initialValue = emptyList())

    // Observe AI adviser state
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.syncingState.collectAsStateWithLifecycle()

    // Navigation and tab selector
    var activeTab by remember { mutableStateOf(0) } // 0: Edge Nodes, 1: platform Sync, 2: AI advice
    val tabs = listOf("Network Edges", "Content Logs", "AI Advisor")

    // State for local asset registration
    var uploadName by remember { mutableStateOf("") }
    var uploadType by remember { mutableStateOf("Video (MP4)") }
    var uploadSizeKb by remember { mutableStateOf(25000) } // 25MB default
    val typeOptions = listOf("Video (MP4)", "Audio (FLAC)", "Image (JPEG)", "JSON Manifest")

    val selectedPlatforms = remember { mutableStateMapOf("YouTube" to true, "Twitch" to false, "Vimeo" to true, "AWS S3" to true) }

    // State for AI Advisor input
    var aiQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Display snackbar updates whenever replication notifications occur
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.resetSyncingState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = SpaceBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "CDN logo",
                        tint = FiberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NEXUS AI CDN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Platform Synchronization Control Console",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(10f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OperationalGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OperationalGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ACTIVE",
                                color = OperationalGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        label = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        icon = {
                            when (index) {
                                0 -> Icon(
                                    imageVector = if (isSelected) Icons.Default.NetworkCheck else Icons.Default.NetworkCheck,
                                    contentDescription = null
                                )
                                1 -> Icon(
                                    imageVector = if (isSelected) Icons.Default.CloudSync else Icons.Default.CloudQueue,
                                    contentDescription = null
                                )
                                2 -> Icon(
                                    imageVector = if (isSelected) Icons.Default.AutoAwesome else Icons.Default.AutoAwesome,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FiberCyan,
                            selectedTextColor = FiberCyan,
                            unselectedTextColor = SlateTextSecondary,
                            unselectedIconColor = SlateTextSecondary,
                            indicatorColor = FiberCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${index}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Live Global CDN metrics strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy.copy(alpha = 0.7f))
                    .padding(vertical = 4.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalBandwidth = nodes.sumOf { it.bandwidthMbps }
                val averageHitRate = if (nodes.isNotEmpty()) nodes.map { it.cacheHitRate }.average() else 0.92
                val healthyCount = nodes.count { it.status == "ONLINE" }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = FiberCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Transit: ${totalBandwidth} Mbps",
                        color = SlateTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "Avg Hit Ratio: ${(averageHitRate * 100).toInt()}%",
                    color = SlateTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Nodes: $healthyCount/${nodes.size} Online",
                    color = if (healthyCount == nodes.size) OperationalGreen else DegradedAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "tab_fade",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { tabIdx ->
                when (tabIdx) {
                    0 -> NetworkEdgesTab(
                        nodes = nodes,
                        onFlushCache = { viewModel.flushNodeCache(it) },
                        onStressTest = { viewModel.simulateEdgeBurst(it) }
                    )
                    1 -> ContentSyncTab(
                        contentItems = contentItems,
                        syncRecords = syncRecords,
                        uploadName = uploadName,
                        onNameChange = { uploadName = it },
                        uploadType = uploadType,
                        onTypeChange = { uploadType = it },
                        uploadSizeKb = uploadSizeKb,
                        onSizeChange = { uploadSizeKb = it },
                        typeOptions = typeOptions,
                        selectedPlatforms = selectedPlatforms,
                        onPlatformToggle = { platform ->
                            selectedPlatforms[platform] = !(selectedPlatforms[platform] ?: false)
                        },
                        onSimulateReplication = {
                            val targets = selectedPlatforms.filter { it.value }.keys.toList()
                            if (uploadName.isNotBlank() && targets.isNotEmpty()) {
                                viewModel.simulateReplication(uploadName, uploadType, uploadSizeKb, targets)
                                uploadName = ""
                            }
                        },
                        onClearLogs = { viewModel.clearHistory() }
                    )
                    2 -> AiOptimizeTab(
                        rules = rules,
                        aiQuery = aiQuery,
                        onQueryChange = { aiQuery = it },
                        aiLoading = aiLoading,
                        aiResponse = aiResponse,
                        onToggleRule = { viewModel.toggleRule(it) },
                        onSubmitQuery = {
                            if (aiQuery.isNotBlank()) {
                                viewModel.askGeminiAdvisor(aiQuery)
                                focusManager.clearFocus()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkEdgesTab(
    nodes: List<CdnEdgeNode>,
    onFlushCache: (CdnEdgeNode) -> Unit,
    onStressTest: (CdnEdgeNode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-tech pulsed World map canvas
        item {
            Column {
                Text(
                    "Reactive Global Server Topology",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                InteractiveNetworkMap(nodes = nodes)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Replicated Datacenter Nodes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlateTextPrimary
                )
                Text(
                    "Live Telemetry",
                    fontSize = 11.sp,
                    color = FiberCyan
                )
            }
        }

        // List of edge servers using card layers
        items(nodes) { node ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("node_card_${node.id}"),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored health state bubble
                        val statusColor = when (node.status) {
                            "ONLINE" -> OperationalGreen
                            "DEGRADED" -> DegradedAmber
                            else -> ErrorCrimson
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = node.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SlateTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = node.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Region: ${node.region} • IP: ${node.ipAddress}",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Node Load", fontSize = 10.sp, color = SlateTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${node.activeLoad}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (node.activeLoad > 85) ErrorCrimson else SlateTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(node.activeLoad / 100f)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(FiberCyan, if (node.activeLoad > 85) ErrorCrimson else IntelligencePurple)
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hit Ratio", fontSize = 10.sp, color = SlateTextSecondary)
                            Text(
                                "${(node.cacheHitRate * 100f).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FiberCyan
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bandwidth", fontSize = 10.sp, color = SlateTextSecondary)
                            Text(
                                "${node.bandwidthMbps} Mbps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onFlushCache(node) },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("flush_cache_btn_${node.id}"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FiberCyan),
                            border = BorderStroke(1.dp, FiberCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flush Cache", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onStressTest(node) },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("stress_test_btn_${node.id}"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IntelligencePurple,
                                contentColor = SlateTextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Speed, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Load Test", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveNetworkMap(nodes: List<CdnEdgeNode>) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulseValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavy)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background coordinate rows
            val dotSpacing = (16.dp.toPx().toInt()).coerceAtLeast(32)
            for (x in 0..size.width.toInt() step dotSpacing) {
                for (y in 0..size.height.toInt() step dotSpacing) {
                    drawCircle(
                        color = BorderColor.copy(alpha = 0.25f),
                        radius = 1.2f,
                        center = Offset(x.toFloat(), y.toFloat())
                    )
                }
            }

            // Standard geographic references
            val serverLocations = listOf(
                Offset(size.width * 0.76f, size.height * 0.35f), // Tokyo
                Offset(size.width * 0.43f, size.height * 0.26f), // London
                Offset(size.width * 0.49f, size.height * 0.32f), // Frankfurt
                Offset(size.width * 0.18f, size.height * 0.34f), // Oregon
                Offset(size.width * 0.84f, size.height * 0.72f), // Sydney
                Offset(size.width * 0.34f, size.height * 0.68f)  // São Paulo
            )

            // Draw inter-connecting fiber link lasers
            for (i in 0 until serverLocations.size) {
                for (j in i + 1 until serverLocations.size) {
                    drawLine(
                        color = FiberCyan.copy(alpha = 0.15f),
                        start = serverLocations[i],
                        end = serverLocations[j],
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw Server edge nodes glow rings
            nodes.forEachIndexed { index, node ->
                if (index < serverLocations.size) {
                    val pos = serverLocations[index]
                    val color = when (node.status) {
                        "ONLINE" -> OperationalGreen
                        "DEGRADED" -> DegradedAmber
                        else -> ErrorCrimson
                    }

                    // Pulsative radar
                    if (node.status == "ONLINE") {
                        drawCircle(
                            color = color.copy(alpha = 0.2f),
                            radius = (12.dp.toPx()) * (1f + (pulseValue * 0.6f)),
                            center = pos
                        )
                    }

                    // Solid Core
                    drawCircle(
                        color = color,
                        radius = 5.dp.toPx(),
                        center = pos
                    )

                    // Draw outer border ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = 6.dp.toPx(),
                        center = pos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Overlay region tags
        nodes.forEachIndexed { index, node ->
            val alignMod = when (index) {
                0 -> Alignment.TopEnd to PaddingValues(top = 18.dp, end = 50.dp)     // Tokyo
                1 -> Alignment.TopCenter to PaddingValues(top = 12.dp, start = 0.dp) // London
                2 -> Alignment.TopCenter to PaddingValues(top = 70.dp, start = 62.dp) // Frankfurt
                3 -> Alignment.TopStart to PaddingValues(top = 34.dp, start = 48.dp)  // Oregon
                4 -> Alignment.BottomEnd to PaddingValues(bottom = 20.dp, end = 40.dp) // Sydney
                5 -> Alignment.BottomCenter to PaddingValues(bottom = 12.dp, end = 80.dp) // São Paulo
                else -> Alignment.Center to PaddingValues(0.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(alignMod.second),
                contentAlignment = alignMod.first
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DeepNavy.copy(alpha = 0.85f))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = node.name.split(" ").lastOrNull() ?: "",
                        fontSize = 9.sp,
                        color = SlateTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ContentSyncTab(
    contentItems: List<ContentItem>,
    syncRecords: List<SyncActivityRecord>,
    uploadName: String,
    onNameChange: (String) -> Unit,
    uploadType: String,
    onTypeChange: (String) -> Unit,
    uploadSizeKb: Int,
    onSizeChange: (Int) -> Unit,
    typeOptions: List<String>,
    selectedPlatforms: Map<String, Boolean>,
    onPlatformToggle: (String) -> Unit,
    onSimulateReplication: () -> Unit,
    onClearLogs: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle item for register assets form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddForm = !showAddForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, null, tint = FiberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Deploy Raw Assets to Global CDN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateTextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand asset form",
                            tint = SlateTextSecondary
                        )
                    }

                    AnimatedVisibility(visible = showAddForm) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = uploadName,
                                onValueChange = onNameChange,
                                label = { Text("Asset Resource Name", color = SlateTextSecondary) },
                                placeholder = { Text("e.g. promo_campaign_4k.mp4") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("asset_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FiberCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = FiberCyan,
                                    unfocusedLabelColor = SlateTextSecondary,
                                    focusedTextColor = SlateTextPrimary,
                                    unfocusedTextColor = SlateTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Mime-Type payload classification", color = SlateTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                typeOptions.forEach { type ->
                                    val isSel = uploadType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) FiberCyan.copy(alpha = 0.25f) else CardBackground)
                                            .border(1.dp, if (isSel) FiberCyan else BorderColor, RoundedCornerShape(6.dp))
                                            .clickable { onTypeChange(type) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            type.split(" ").firstOrNull() ?: "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) FiberCyan else SlateTextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Asset Size Allocation: ${(uploadSizeKb / 1000f).toInt()} MB", color = SlateTextSecondary, fontSize = 12.sp)
                            Slider(
                                value = uploadSizeKb.toFloat(),
                                onValueChange = { onSizeChange(it.toInt()) },
                                valueRange = 1000f..500000f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = FiberCyan,
                                    activeTrackColor = FiberCyan,
                                    inactiveTrackColor = BorderColor
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Multi-Channel Sync Targets", color = SlateTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                selectedPlatforms.forEach { (platform, isEnabled) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            onPlatformToggle(platform)
                                        }
                                    ) {
                                        Checkbox(
                                            checked = isEnabled,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = FiberCyan,
                                                uncheckedColor = SlateTextSecondary,
                                                checkmarkColor = SpaceBlack
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(platform, fontSize = 11.sp, color = SlateTextPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onSimulateReplication,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("broadcast_replicate_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FiberCyan,
                                    contentColor = SpaceBlack
                                ),
                                enabled = uploadName.isNotBlank() && selectedPlatforms.any { it.value }
                            ) {
                                Icon(Icons.Default.CloudSync, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trigger Global Replication Blast", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "CDN Registered Inventory Assets",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SlateTextPrimary
            )
        }

        if (contentItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepNavy.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = SlateTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No registered inventory items in local database storage.", fontSize = 12.sp, color = SlateTextSecondary)
                    }
                }
            }
        }

        items(contentItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${item.mimeType} • ${(item.sizeKb / 1000f).toInt()} MB",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }

                        val statusColor = when (item.syncStatus) {
                            "COMPLETED" -> OperationalGreen
                            "ACTIVE" -> FiberCyan
                            "FAILED" -> ErrorCrimson
                            else -> SlateTextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                item.syncStatus,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Channels: ${item.targetPlatforms}",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { item.progress / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (item.syncStatus == "COMPLETED") OperationalGreen else FiberCyan,
                            trackColor = BorderColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "${item.progress}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }
                }
            }
        }

        // Real-time synchronization Terminal logs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "High-Throughput Sync Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlateTextPrimary
                )
                if (syncRecords.isNotEmpty()) {
                    Text(
                        "Flush Logs",
                        color = ErrorCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearLogs() }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (syncRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepNavy.copy(alpha = 0.4f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Log console silent. Awaiting network action.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SlateTextSecondary
                    )
                }
            }
        }

        items(syncRecords) { log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DeepNavy)
                    .border(0.5.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (log.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Pending,
                    contentDescription = null,
                    tint = if (log.status == "COMPLETED") OperationalGreen else FiberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            log.contentItemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlateTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            log.platform,
                            fontSize = 10.sp,
                            color = IntelligencePurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Throughput: ${log.speedMbps.toInt()} Mbps • progress: ${log.progress}%",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiOptimizeTab(
    rules: List<AiOptimizationRule>,
    aiQuery: String,
    onQueryChange: (String) -> Unit,
    aiLoading: Boolean,
    aiResponse: String?,
    onToggleRule: (AiOptimizationRule) -> Unit,
    onSubmitQuery: () -> Unit
) {
    var activeFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pre-warming", "Edge Compression", "Intelligent Routing")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Optimization Assistant panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = IntelligencePurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gemini-3.5 CDN Advisor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SlateTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Ask the built-in model to compile dynamic configuration manifests, analyze traffic, or design failover routes.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = aiQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("e.g. Audit current Oregon loads & compile an failover manifest", color = SlateTextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_advisor_search_input"),
                        trailingIcon = {
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FiberCyan)
                            } else {
                                IconButton(onClick = onSubmitQuery, modifier = Modifier.testTag("send_query_btn")) {
                                    Icon(Icons.Default.Send, null, tint = FiberCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary,
                            focusedBorderColor = FiberCyan,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset prompt chips
                    Text(
                        "Suggested Audit Prompts:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val prompts = listOf(
                            "Optimize Tokyo routing rules",
                            "Suggest failsafes for São Paulo",
                            "Edge Compression Policy check"
                        )
                        prompts.forEach { text ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackground)
                                    .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .clickable { onQueryChange(text) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text, fontSize = 10.sp, color = FiberCyan)
                            }
                        }
                    }

                    AnimatedVisibility(visible = aiResponse != null) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = BorderColor)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Recommendation Payload", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IntelligencePurple)
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpaceBlack)
                                    .border(1.dp, IntelligencePurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiResponse ?: "",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SlateTextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "AI Optimization Policies & Rules",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SlateTextPrimary
            )
        }

        // Filters categories
        item {
            Row(
                modifier = Modifier
                    .fillModifier()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSel = activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) FiberCyan.copy(alpha = 0.2f) else CardBackground)
                            .border(1.dp, if (isSel) FiberCyan else BorderColor, RoundedCornerShape(16.dp))
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            filter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) FiberCyan else SlateTextSecondary
                        )
                    }
                }
            }
        }

        // List rules
        val filteredRules = if (activeFilter == "All") rules else rules.filter { it.ruleType == activeFilter }
        items(filteredRules) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rule.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SlateTextPrimary
                            )
                            Text(
                                rule.ruleType,
                                fontSize = 11.sp,
                                color = FiberCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = rule.isActive,
                            onCheckedChange = { onToggleRule(rule) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpaceBlack,
                                checkedTrackColor = FiberCyan,
                                uncheckedThumbColor = SlateTextSecondary,
                                uncheckedTrackColor = CardBackground
                            ),
                            modifier = Modifier.testTag("rule_toggle_${rule.id}")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        rule.description,
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Confidence: ${(rule.confidenceScore * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SlateTextSecondary
                        )
                        Text(
                            "Saved bandwidth: ${rule.estimatedSavingsGb.toInt()} GB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OperationalGreen
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()
