package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.ScanLog
import com.example.data.SecurityThreat
import com.example.engine.ScanProgressState
import com.example.ui.AntivirusViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

object Dest {
    const val DASHBOARD = "dashboard"
    const val APP_ANALYZER = "app_analyzer"
    const val ADVISOR = "advisor"
    const val THREAT_CENTER = "threat_center"
    const val LOGS = "logs"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntivirusMainScreen(viewModel: AntivirusViewModel) {
    val navController = rememberNavController()
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()
    val scanProgressState by viewModel.scanProgress.collectAsStateWithLifecycle()
    
    val currentRoute = remember { mutableStateOf(Dest.DASHBOARD) }

    // Synchronize bottom bar state with navigation
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentRoute.value = destination.route ?: Dest.DASHBOARD
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    Scaffold(
        bottomBar = {
            // Standard NavigationBar respecting systemic safe drawings
            NavigationBar(
                containerColor = SlateMedium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(0.5.dp, SlateLight.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentRoute.value == Dest.DASHBOARD,
                    onClick = { navController.navigate(Dest.DASHBOARD) { popUpTo(Dest.DASHBOARD) { saveState = true }; launchSingleTop = true } },
                    icon = { Icon(if (currentRoute.value == Dest.DASHBOARD) Icons.Filled.Shield else Icons.Outlined.Shield, contentDescription = "Shield Home") },
                    label = { Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldCyan,
                        selectedTextColor = ShieldCyan,
                        indicatorColor = Color(0xFFD1E4FF),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentRoute.value == Dest.APP_ANALYZER,
                    onClick = { navController.navigate(Dest.APP_ANALYZER) { launchSingleTop = true } },
                    icon = {
                        BadgedBox(badge = {
                            if (activeThreats.filter { it.threatType == "APP" }.isNotEmpty()) {
                                Badge(containerColor = ShieldRose) {
                                    Text(activeThreats.filter { it.threatType == "APP" }.size.toString(), color = Color.White)
                                }
                            }
                        }) {
                            Icon(if (currentRoute.value == Dest.APP_ANALYZER) Icons.Filled.AppSettingsAlt else Icons.Outlined.AppSettingsAlt, contentDescription = "App Analyzer")
                        }
                    },
                    label = { Text("Apps", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldCyan,
                        selectedTextColor = ShieldCyan,
                        indicatorColor = Color(0xFFD1E4FF),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentRoute.value == Dest.ADVISOR,
                    onClick = { navController.navigate(Dest.ADVISOR) { launchSingleTop = true } },
                    icon = {
                        BadgedBox(badge = {
                            if (activeThreats.filter { it.threatType == "VULNERABILITY" }.isNotEmpty()) {
                                Badge(containerColor = ShieldAmber) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(8.dp), tint = SlateDark)
                                }
                            }
                        }) {
                            Icon(if (currentRoute.value == Dest.ADVISOR) Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser, contentDescription = "Advisor")
                        }
                    },
                    label = { Text("System", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldCyan,
                        selectedTextColor = ShieldCyan,
                        indicatorColor = Color(0xFFD1E4FF),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentRoute.value == Dest.THREAT_CENTER,
                    onClick = { navController.navigate(Dest.THREAT_CENTER) { launchSingleTop = true } },
                    icon = {
                        BadgedBox(badge = {
                            val totalRisks = activeThreats.size
                            if (totalRisks > 0) {
                                Badge(containerColor = ShieldRose) {
                                    Text(totalRisks.toString(), color = Color.White)
                                }
                            }
                        }) {
                            Icon(if (currentRoute.value == Dest.THREAT_CENTER) Icons.Filled.Dangerous else Icons.Outlined.Dangerous, contentDescription = "Threats")
                        }
                    },
                    label = { Text("Threats", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldCyan,
                        selectedTextColor = ShieldCyan,
                        indicatorColor = Color(0xFFD1E4FF),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = currentRoute.value == Dest.LOGS,
                    onClick = { navController.navigate(Dest.LOGS) { launchSingleTop = true } },
                    icon = { Icon(if (currentRoute.value == Dest.LOGS) Icons.Filled.History else Icons.Outlined.History, contentDescription = "History Logs") },
                    label = { Text("Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ShieldCyan,
                        selectedTextColor = ShieldCyan,
                        indicatorColor = Color(0xFFD1E4FF),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Dest.DASHBOARD,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Dest.DASHBOARD) { DashboardScreen(viewModel) }
                composable(Dest.APP_ANALYZER) { AppAnalyzerScreen(viewModel) }
                composable(Dest.ADVISOR) { SecurityAdvisorScreen(viewModel) }
                composable(Dest.THREAT_CENTER) { ThreatCenterScreen(viewModel) }
                composable(Dest.LOGS) { ScanLogsScreen(viewModel) }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AntivirusViewModel) {
    val realTimeEnabled by viewModel.isRealTimeProtectionEnabled.collectAsStateWithLifecycle()
    val scanLogs by viewModel.scanLogs.collectAsStateWithLifecycle()
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SENTINEL SECURE",
                        style = MaterialTheme.typography.labelMedium,
                        color = ShieldCyan,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Threat Engine v2.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    color = SlateMedium,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (realTimeEnabled) ShieldGreen else ShieldRose)
                        )
                        Text(
                            text = if (realTimeEnabled) "LIVE ACTIVE" else "LIVE OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (realTimeEnabled) ShieldGreen else ShieldRose
                        )
                    }
                }
            }
        }

        // Radar Shield Dial Section
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(SlateMedium.copy(alpha = 0.5f))
                    .border(2.dp, SlateLight.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Radar scanning or pulsing
                when (val progress = scanProgress) {
                    is ScanProgressState.Scanning -> {
                        AnimatedRadarSweep()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "${(progress.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = ShieldCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${progress.scannedCount} Objects",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (progress.threatsFound > 0) "${progress.threatsFound} RISK FILED" else "Healthy",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (progress.threatsFound > 0) ShieldRose else ShieldGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    is ScanProgressState.Completed -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = if (progress.threats.isEmpty()) Icons.Filled.GppGood else Icons.Filled.GppMaybe,
                                contentDescription = "Scan status",
                                modifier = Modifier.size(54.dp),
                                tint = if (progress.threats.isEmpty()) ShieldGreen else ShieldRose
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (progress.threats.isEmpty()) "HEALTHY" else "RISK NOTED",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (progress.threats.isEmpty()) ShieldGreen else ShieldRose,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.resetScanState() },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateLight),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Dismiss", fontSize = 11.sp, color = TextLight)
                            }
                        }
                    }
                    else -> {
                        // Pulse circle for idle state
                        PulseShieldIndicator(activeThreats.size)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = if (activeThreats.isEmpty()) Icons.Filled.Shield else Icons.Filled.Warning,
                                contentDescription = "Shield Indicator",
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("shield_logo"),
                                tint = if (activeThreats.isEmpty()) ShieldGreen else ShieldRose
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (activeThreats.isEmpty()) "SECURE" else "ALERT STATUS",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (activeThreats.isEmpty()) ShieldGreen else ShieldRose,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        // Live status progress reporter label
        item {
            when (val progress = scanProgress) {
                is ScanProgressState.Scanning -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ShieldCyan,
                            trackColor = SlateLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progress.currentItemName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {}
            }
        }

        // Scanning Triggers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.triggerScan("QUICK") },
                    enabled = scanProgress !is ScanProgressState.Scanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShieldCyan,
                        disabledContainerColor = SlateLight
                    )
                ) {
                    Icon(Icons.Filled.FlashOn, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Scan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }

                Button(
                    onClick = { viewModel.triggerScan("FULL") },
                    enabled = scanProgress !is ScanProgressState.Scanning,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("full_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeThreats.isNotEmpty()) ShieldRose else SlateLight,
                        disabledContainerColor = SlateLight
                    )
                ) {
                    Icon(
                        imageVector = if (activeThreats.isNotEmpty()) Icons.Filled.SecurityUpdateWarning else Icons.Filled.Policy,
                        contentDescription = null,
                        tint = if (activeThreats.isNotEmpty()) Color.White else ShieldGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Full System Scan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Action Toggles Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SlateLight.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(ShieldCyan.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Real-Time Shields", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight)
                                Text("Active heuristic malware trap", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = realTimeEnabled,
                            onCheckedChange = { viewModel.toggleRealTimeProtection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateMedium,
                                checkedTrackColor = ShieldGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateLight
                            )
                        )
                    }

                    HorizontalDivider(color = SlateLight.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(ShieldAmber.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Update, contentDescription = null, tint = ShieldAmber, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Scanner Definitions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight)
                                Text("Last checked: Today", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateLight.copy(alpha = 0.8f)
                        ) {
                            Text("Latest v241", fontSize = 10.sp, color = ShieldAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Summary Card of threats (if dynamic threat exists)
        if (activeThreats.isNotEmpty()) {
            item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ShieldRose.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, ShieldRose.copy(alpha = 0.4f))
                            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = ShieldRose, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${activeThreats.size} Risks Detected on Device", fontWeight = FontWeight.ExtraBold, color = ShieldRose, fontSize = 14.sp)
                            Text("Your immediate attention is requested to isolate package entries or delete file caches.", fontSize = 11.sp, color = TextLight.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // Last scan timestamp logger
        item {
            val logs = scanLogs
            if (logs.isNotEmpty()) {
                val lastLog = logs.first()
                Text(
                    text = "Last complete scan: " + DateUtils.getRelativeTimeSpanString(lastLog.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                Text(
                    text = "First scan has not been completed yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun AppAnalyzerScreen(viewModel: AntivirusViewModel) {
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()
    val appThreats = activeThreats.filter { it.threatType == "APP" }
    
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "App Threat Guard & Signatures",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = TextLight,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Analyze installed packages using heuristic behavior patterns or verify signature fingerprints against our unrecognized malicious database.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom segment-selector tab switcher 
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .background(SlateMedium, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SlateLight.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(if (selectedTab == 0) ShieldCyan else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { selectedTab = 0 }
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(
                    text = "Heuristic Risks (${appThreats.size})",
                    color = if (selectedTab == 0) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(if (selectedTab == 1) ShieldCyan else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { selectedTab = 1 }
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(
                    text = "Signature Database",
                    color = if (selectedTab == 1) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (appThreats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(ShieldGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ShieldGreen, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Heuristic Threats Identified", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("All apps adhere to safe behavioral guidelines.", fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(appThreats) { threat ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("app_threat_card_${threat.referenceKey}"),
                            colors = CardDefaults.cardColors(containerColor = SlateMedium),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ShieldRose.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(ShieldRose.copy(alpha = 0.1F), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Android, contentDescription = null, tint = ShieldRose, modifier = Modifier.size(24.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(threat.name, fontWeight = FontWeight.Black, color = TextLight, fontSize = 16.sp)
                                        Text(threat.referenceKey, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Surface(
                                        color = if (threat.severity == "CRITICAL") ShieldRose else ShieldAmber,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(threat.severity, color = SlateDark, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = threat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLight.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.whitelistThreat(threat) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SlateLight),
                                        modifier = Modifier.height(36.dp).weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                                    ) {
                                        Text("Trust App", fontSize = 11.sp)
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.deleteThreatItem(threat) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(36.dp).weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ShieldRose)
                                    ) {
                                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Uninstall App", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Signature verification Database view
            val signaturesList by viewModel.installedAppSignatures.collectAsStateWithLifecycle()
            val filteredList = signaturesList.filter {
                it.label.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true)
            }

            // Central Signature DB stats summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateMedium.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateLight.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ShieldCyan.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Central Signature Database",
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "3 recorded malicious signature profiles stored | v2026.06_PRO",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by app name or package...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("signature_search_box"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ShieldCyan,
                    unfocusedBorderColor = SlateLight.copy(alpha = 0.3f),
                    focusedContainerColor = SlateMedium,
                    unfocusedContainerColor = SlateMedium
                ),
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        EmptyStatePlaceholder(
                            title = "No Matches Found",
                            subtitle = "Verify search parameters or look up using standard heuristics."
                        )
                    }
                } else {
                    items(filteredList) { appSig ->
                        val isMalicious = appSig.isMaliciousMatch
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sig_card_${appSig.packageName}"),
                            colors = CardDefaults.cardColors(containerColor = SlateMedium),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isMalicious) ShieldRose.copy(alpha = 0.4f) else SlateLight.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                if (isMalicious) ShieldRose.copy(alpha = 0.12f) else ShieldGreen.copy(alpha = 0.12f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isMalicious) Icons.Filled.Warning else Icons.Filled.Fingerprint,
                                            contentDescription = null,
                                            tint = if (isMalicious) ShieldRose else ShieldGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appSig.label,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = appSig.packageName,
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = if (isMalicious) ShieldRose.copy(alpha = 0.15f) else ShieldGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isMalicious) "SIGNATURE MATCH" else "VERIFIED SAFE",
                                            color = if (isMalicious) ShieldRose else ShieldGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = SlateLight.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Certificate SHA-256 Fingerprint:",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = if (appSig.isSystem) "System Certificate" else "User Certificate",
                                        fontSize = 9.sp,
                                        color = ShieldCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = appSig.signatureHash.ifEmpty { "SIMULATED_DEBUG_DEVELOPER_KEY_SHA256" },
                                    fontSize = 10.sp,
                                    color = if (isMalicious) ShieldRose else TextLight.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .background(SlateLight.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                )

                                if (isMalicious && appSig.description != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = ShieldRose.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ShieldRose.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Matched Strain: ${appSig.detectionInfo ?: "Malware"}",
                                                fontSize = 11.sp,
                                                color = ShieldRose,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = appSig.description,
                                                fontSize = 10.sp,
                                                color = TextLight.copy(alpha = 0.8f),
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityAdvisorScreen(viewModel: AntivirusViewModel) {
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()
    val simulatedRoot by viewModel.simulatedRootState.collectAsStateWithLifecycle()
    val rootUninstalling by viewModel.rootUninstalling.collectAsStateWithLifecycle()
    val rootUninstallLogs by viewModel.rootUninstallLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val vulnerabilityThreats = activeThreats.filter { it.threatType == "VULNERABILITY" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "System Security Advisor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextLight,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Evaluates baseline Android security postures, lock state settings, and hardware interface exposures.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            HorizontalDivider(color = SlateLight.copy(alpha = 0.5f))
        }

        // KernelSU & Root Lab Segment Board
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("kernelsu_lab_card"),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ShieldCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ShieldCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.BugReport, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Kernel & SU Simulation Lab",
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Test-drive root protection behaviors safely",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SIMULATED ENVIRONMENT ROOT CONFIGURATION:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ShieldCyan,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 4-way Segmented Button Style layout
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val options = listOf(
                            Triple("NONE", "Verified Safe", ShieldGreen),
                            Triple("KERNELSU", "KernelSU Root", ShieldCyan),
                            Triple("MAGISK", "Magisk Root", ShieldAmber),
                            Triple("GENERIC", "Generic SU Root", ShieldRose)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.take(2).forEach { (stateKey, displayName, accentColor) ->
                                val isSelected = simulatedRoot == stateKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.15f) else SlateLight.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) accentColor else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { 
                                            viewModel.updateSimulatedRootState(stateKey) 
                                            viewModel.triggerScan("QUICK")
                                        }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) accentColor else TextLight,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.drop(2).take(2).forEach { (stateKey, displayName, accentColor) ->
                                val isSelected = simulatedRoot == stateKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.15f) else SlateLight.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) accentColor else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { 
                                            viewModel.updateSimulatedRootState(stateKey) 
                                            viewModel.triggerScan("QUICK")
                                        }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) accentColor else TextLight,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SlateLight.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HARDWARE & FILESYSTEM SIGNALS:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Diagnostic lines
                    val pm = context.packageManager
                    val sysKsuExists = java.io.File("/sys/module/kernelsu").exists() || java.io.File("/sys/kernel/kfcf").exists()
                    val sysManagerInstalled = try { pm.getPackageInfo("me.weishu.kernelsu", 0); true } catch (e: Exception) { false }
                    val sysKsuFolderExists = java.io.File("/data/adb/ksu").exists()
                    
                    DiagnosticRow(
                        title = "/sys Module Parameters Node Detect",
                        passed = !sysKsuExists,
                        simulatedTypeLabel = if (simulatedRoot == "KERNELSU") "SIMULATED POSITIVE" else "SECURE"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticRow(
                        title = "me.weishu.kernelsu Official Manager App Query",
                        passed = !sysManagerInstalled,
                        simulatedTypeLabel = if (simulatedRoot == "KERNELSU") "SIMULATED POSITIVE" else "SECURE"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticRow(
                        title = "Work Dir /data/adb/ksu Node Check",
                        passed = !sysKsuFolderExists,
                        simulatedTypeLabel = if (simulatedRoot == "KERNELSU") "SIMULATED POSITIVE" else "SECURE"
                    )
                }
            }
        }

        if (vulnerabilityThreats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ShieldGreen.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = ShieldGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All Checks Succeeded", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        Text("No active vulnerabilities detected. Lock screen is secure and testing tools are locked down.", fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(vulnerabilityThreats) { threat ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("vulnerability_card_${threat.referenceKey}"),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (threat.referenceKey == "VULN_ROOT") ShieldRose.copy(alpha = 0.3f) else ShieldAmber.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background((if (threat.referenceKey == "VULN_ROOT") ShieldRose else ShieldAmber).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (threat.referenceKey == "VULN_ROOT") Icons.Filled.Report else Icons.Filled.AdminPanelSettings,
                                    contentDescription = null, 
                                    tint = if (threat.referenceKey == "VULN_ROOT") ShieldRose else ShieldAmber, 
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(threat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight)
                                    Text(
                                        text = if (threat.referenceKey == "VULN_ROOT") "CRITICAL" else "AFFECTED",
                                        color = if (threat.referenceKey == "VULN_ROOT") ShieldRose else ShieldAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(threat.description, fontSize = 11.sp, color = TextLight.copy(alpha = 0.85f), lineHeight = 14.sp)
                            }
                        }

                        if (threat.referenceKey == "VULN_ROOT") {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = SlateLight.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (rootUninstalling) {
                                // Live uninstallation console logs
                                Text(
                                    text = "REMEDY TERMINAL CONSOLE:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ShieldCyan,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, SlateLight.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(rootUninstallLogs) { log ->
                                            Text(
                                                text = log,
                                                color = if (log.contains("✅") || log.contains("✨")) Color(0xFF10B981) else if (log.contains("🔍")) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                            )
                                        }
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(10.dp),
                                                    color = Color(0xFF38BDF8),
                                                    strokeWidth = 1.5.dp
                                                )
                                                Text(
                                                    text = "processing...",
                                                    color = Color(0xFF64748B),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.runRootUninstallation(threat.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldRose),
                                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("uninstall_root_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Uninstall Root Module & Repair Filesystem", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateLight.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                            Text("Hardware / Device Uninstallation Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextLight)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "On real physical terminals, third-party user-space apps cannot silently flash partition tables or alter the Linux kernel binary in-flight. Click above to purge simulation variables immediately, or follow standard manual procedures (bootloader recovery mode, restore original factory boot.img layers, or run official 'Uninstall' in Magisk/KernelSU).",
                                            fontSize = 9.sp,
                                            color = TextMuted,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(title: String, passed: Boolean, simulatedTypeLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = TextLight.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (passed && simulatedTypeLabel == "SECURE") ShieldGreen else ShieldRose)
            )
            Text(
                text = if (passed && simulatedTypeLabel == "SECURE") "SECURE (ABSENT)" else simulatedTypeLabel,
                color = if (passed && simulatedTypeLabel == "SECURE") ShieldGreen else ShieldRose,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ThreatCenterScreen(viewModel: AntivirusViewModel) {
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()
    val quarantinedThreats by viewModel.quarantinedThreats.collectAsStateWithLifecycle()
    val whitelistedThreats by viewModel.whitelistedThreats.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Risk Management",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = TextLight,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Quarantine suspicious objects to sandbox files, or whitelist trusted tools.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SlateMedium,
            contentColor = ShieldCyan,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Detected (${activeThreats.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Isolated (${quarantinedThreats.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Whitelisted (${whitelistedThreats.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Detected Risks
                    if (activeThreats.isEmpty()) {
                        item {
                            EmptyStatePlaceholder("All Clean", "No threats detected on this module.")
                        }
                    } else {
                        items(activeThreats) { threat ->
                            ThreatRowItem(
                                threat = threat,
                                onActionOne = { viewModel.quarantineThreat(threat) },
                                actionOneLabel = "Quarantine",
                                onActionTwo = { viewModel.deleteThreatItem(threat) },
                                actionTwoLabel = if (threat.threatType == "APP") "Uninstall" else "Delete"
                            )
                        }
                    }
                }
                1 -> {
                    // Isolated (Quarantined)
                    if (quarantinedThreats.isEmpty()) {
                        item {
                            EmptyStatePlaceholder("Isolation Chamber Empty", "Files quarantined will be relocated here securely.")
                        }
                    } else {
                        items(quarantinedThreats) { threat ->
                            ThreatRowItem(
                                threat = threat,
                                onActionOne = { viewModel.restoreQuarantinedThreat(threat) },
                                actionOneLabel = "Restore File",
                                onActionTwo = { viewModel.deleteThreatItem(threat) },
                                actionTwoLabel = "Erase Data"
                            )
                        }
                    }
                }
                2 -> {
                    // Whitelisted (Ignored)
                    if (whitelistedThreats.isEmpty()) {
                        item {
                            EmptyStatePlaceholder("Whitelist Empty", "Ignored apps and files accumulate here.")
                        }
                    } else {
                        items(whitelistedThreats) { threat ->
                            ThreatRowItem(
                                threat = threat,
                                onActionOne = { viewModel.removeThreatFromWhitelist(threat) },
                                actionOneLabel = "Revoke Trust",
                                onActionTwo = {},
                                actionTwoLabel = ""
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatRowItem(
    threat: SecurityThreat,
    onActionOne: () -> Unit,
    actionOneLabel: String,
    onActionTwo: () -> Unit,
    actionTwoLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, SlateLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (threat.severity == "CRITICAL") ShieldRose.copy(alpha = 0.15f) else ShieldAmber.copy(
                                alpha = 0.15f
                            ), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (threat.threatType == "APP") Icons.Filled.AppBlocking else Icons.Filled.Dangerous,
                        contentDescription = null,
                        tint = if (threat.severity == "CRITICAL") ShieldRose else ShieldAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(threat.name, fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                    Text(threat.referenceKey, fontSize = 10.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Surface(
                    color = SlateLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(threat.threatType, fontSize = 9.sp, color = TextLight, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(threat.description, fontSize = 11.sp, color = TextLight.copy(alpha = 0.85f), lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (actionOneLabel.isNotEmpty()) {
                    Button(
                        onClick = onActionOne,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateLight)
                    ) {
                        Text(actionOneLabel, fontSize = 11.sp, color = TextLight)
                    }
                }
                if (actionTwoLabel.isNotEmpty()) {
                    Button(
                        onClick = onActionTwo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldRose.copy(alpha = 0.9f))
                    ) {
                        Text(actionTwoLabel, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanLogsScreen(viewModel: AntivirusViewModel) {
    val logs by viewModel.scanLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Security Audit Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextLight
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllLogs() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = ShieldRose, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = ShieldRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = "Historical details on scanning activity, completed checks, durations, and logs.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        HorizontalDivider(color = SlateLight.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            EmptyStatePlaceholder("No Scan Audits Complete", "Complete your initial Quick or Full Security audit to compile logs here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateMedium),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (log.threatsFound > 0) ShieldRose.copy(alpha = 0.15f) else ShieldGreen.copy(
                                            alpha = 0.15f
                                        ), CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.threatsFound > 0) Icons.Filled.GppMaybe else Icons.Filled.GppGood,
                                    contentDescription = null,
                                    tint = if (log.threatsFound > 0) ShieldRose else ShieldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${log.scanType} Scan completed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextLight
                                    )
                                    val formattedDate = remember(log.timestamp) {
                                        SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(log.timestamp))
                                    }
                                    Text(formattedDate, fontSize = 10.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Checked ${log.itemsScanned} elements in ${log.durationMs}ms • Found ${log.threatsFound} threats",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = SlateLight)
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AnimatedRadarSweep() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.3f

        // Technical radar concentric grid rings
        drawCircle(
            color = ShieldCyan.copy(alpha = 0.08f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = ShieldCyan.copy(alpha = 0.12f),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = ShieldCyan.copy(alpha = 0.18f),
            radius = radius * 0.3f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Crosshairs lines
        drawLine(
            color = ShieldCyan.copy(alpha = 0.1f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = ShieldCyan.copy(alpha = 0.1f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )

        // Animated rotating radar brush sweep
        val angleRad = Math.toRadians(rotationAngle.toDouble())
        val scanLineEndX = center.x + radius * Math.cos(angleRad).toFloat()
        val scanLineEndY = center.y + radius * Math.sin(angleRad).toFloat()

        drawLine(
            brush = Brush.sweepGradient(
                colors = listOf(
                    ShieldCyan.copy(alpha = 0.4f),
                    ShieldCyan.copy(alpha = 0.01f),
                    Color.Transparent
                ),
                center = center
            ),
            start = center,
            end = Offset(scanLineEndX, scanLineEndY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PulseShieldIndicator(threatCount: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val pulseColor = if (threatCount > 0) ShieldRose else ShieldGreen

    Box(
        modifier = Modifier
            .size(190.dp)
            .scale(scaleFactor)
            .background(pulseColor.copy(alpha = 0.05f), CircleShape)
            .border(1.2.dp, pulseColor.copy(alpha = 0.25f), CircleShape)
    )
}
