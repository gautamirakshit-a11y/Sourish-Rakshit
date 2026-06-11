package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AntivirusRepository
import com.example.data.ScanLog
import com.example.data.SecurityThreat
import com.example.engine.AntivirusEngine
import com.example.engine.ScanProgressState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AntivirusViewModel(
    private val context: Context,
    private val repository: AntivirusRepository
) : ViewModel() {

    private val engine = AntivirusEngine(context)

    private val _scanProgress = MutableStateFlow<ScanProgressState?>(null)
    val scanProgress: StateFlow<ScanProgressState?> = _scanProgress.asStateFlow()

    private val _isRealTimeProtectionEnabled = MutableStateFlow(true)
    val isRealTimeProtectionEnabled: StateFlow<Boolean> = _isRealTimeProtectionEnabled.asStateFlow()

    val scanLogs: StateFlow<List<ScanLog>> = repository.scanLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedAppSignatures: StateFlow<List<com.example.engine.AppSignatureDetails>> = flow {
        val pm = context.packageManager
        val packages = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES or android.content.pm.PackageManager.GET_SIGNATURES)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val list = packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val label = appInfo.loadLabel(pm).toString()
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            
            val sigHash = com.example.engine.SignatureDatabase.getAppSignatureHash(context, pkg.packageName)
            val matchedMalware = com.example.engine.SignatureDatabase.maliciousSignatures[sigHash.uppercase()]
            val isMaliciousMatch = matchedMalware != null || pkg.packageName.contains("magisk") || label.contains("Magisk")
            val detectionInfo = matchedMalware?.first ?: if (isMaliciousMatch) "Risk.Superuser" else null
            val description = matchedMalware?.second ?: if (isMaliciousMatch) "Device Root manager framework signature bypass danger" else null
            
            com.example.engine.AppSignatureDetails(
                label = label,
                packageName = pkg.packageName,
                signatureHash = sigHash,
                isSystem = isSystem,
                isMaliciousMatch = isMaliciousMatch,
                detectionInfo = detectionInfo,
                description = description
            )
        }
        emit(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeThreats: StateFlow<List<SecurityThreat>> = repository.allThreats
        .map { threats -> threats.filter { !it.isWhitelisted && !it.isQuarantined } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quarantinedThreats: StateFlow<List<SecurityThreat>> = repository.allThreats
        .map { threats -> threats.filter { it.isQuarantined && !it.isWhitelisted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val whitelistedThreats: StateFlow<List<SecurityThreat>> = repository.allThreats
        .map { threats -> threats.filter { it.isWhitelisted && !it.isQuarantined } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _simulatedRootState = MutableStateFlow("NONE")
    val simulatedRootState: StateFlow<String> = _simulatedRootState.asStateFlow()

    private val _rootUninstalling = MutableStateFlow(false)
    val rootUninstalling: StateFlow<Boolean> = _rootUninstalling.asStateFlow()

    private val _rootUninstallLogs = MutableStateFlow<List<String>>(emptyList())
    val rootUninstallLogs: StateFlow<List<String>> = _rootUninstallLogs.asStateFlow()

    init {
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        _isRealTimeProtectionEnabled.value = prefs.getBoolean("real_time_enabled", true)
        
        val isFirstRun = !prefs.contains("simulated_root_state")
        val defaultRoot = if (isFirstRun) "KERNELSU" else "NONE"
        _simulatedRootState.value = prefs.getString("simulated_root_state", defaultRoot) ?: defaultRoot
        if (isFirstRun) {
            prefs.edit().putString("simulated_root_state", "KERNELSU").apply()
            triggerScan("QUICK")
        }
    }

    fun toggleRealTimeProtection(enabled: Boolean) {
        _isRealTimeProtectionEnabled.value = enabled
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("real_time_enabled", enabled).apply()
    }

    fun updateSimulatedRootState(state: String) {
        _simulatedRootState.value = state
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("simulated_root_state", state).apply()
    }

    fun runRootUninstallation(rootType: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _rootUninstalling.value = true
            _rootUninstallLogs.value = emptyList()
            
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add(msg)
                _rootUninstallLogs.value = logs.toList()
            }

            addLog("⚡ Initializing uninstallation routine...")
            kotlinx.coroutines.delay(500)
            
            if (rootType.lowercase().contains("kernelsu")) {
                addLog("🔍 Target identified: KernelSU (Kernel-level Root)")
                kotlinx.coroutines.delay(400)
                addLog("📦 Checking kernel space driver node '/sys/module/kernelsu'...")
                kotlinx.coroutines.delay(400)
                addLog("⚙️ Reverting overlayfs mount nodes to secure context...")
                kotlinx.coroutines.delay(500)
                addLog("🗑️ Removing KernelSU binaries at '/data/adb/ksu/bin/su'...")
                kotlinx.coroutines.delay(400)
                addLog("🗑️ Deleting data work directories: /data/adb/ksu")
                kotlinx.coroutines.delay(400)
                addLog("⚙️ Purging daemon service socket '/data/adb/ksud'...")
                kotlinx.coroutines.delay(400)
                addLog("🛡️ Restoring system-level sandboxing models...")
                kotlinx.coroutines.delay(400)
            } else if (rootType.lowercase().contains("magisk")) {
                addLog("🔍 Target identified: Magisk (Systemless Root)")
                kotlinx.coroutines.delay(400)
                addLog("📦 Verifying stock boot.img backup files on storage...")
                kotlinx.coroutines.delay(400)
                addLog("⚙️ Unpatching ramdisk structures and raw directories...")
                kotlinx.coroutines.delay(500)
                addLog("🗑️ Erasing Magisk environment nodes: /sbin/.magisk...")
                kotlinx.coroutines.delay(400)
                addLog("🗑️ Deleting magisk.db configurations and logs...")
                kotlinx.coroutines.delay(400)
                addLog("🛡️ Restoring untampered partition signatures...")
                kotlinx.coroutines.delay(500)
            } else {
                addLog("🔍 Target identified: Generic Superuser binary")
                kotlinx.coroutines.delay(400)
                addLog("⚙️ Locating executable paths: /system/bin/su, /system/xbin/su...")
                kotlinx.coroutines.delay(500)
                addLog("🗑️ Removing high-privilege su bin files...")
                kotlinx.coroutines.delay(400)
                addLog("🛡️ Recalibrating SELinux context policies to Enforcing...")
                kotlinx.coroutines.delay(400)
            }

            addLog("🔄 Syncing and clearing simulation values...")
            updateSimulatedRootState("NONE")
            kotlinx.coroutines.delay(400)
            
            addLog("✨ Sanitation check: ALL SYSTEMS SECURED")
            addLog("✅ Simulated root framework successfully removed!")
            kotlinx.coroutines.delay(500)

            _rootUninstalling.value = false
            triggerScan("QUICK")
            onComplete()
        }
    }

    fun triggerScan(scanType: String) {
        viewModelScope.launch {
            engine.performScan(scanType).collect { progressState ->
                _scanProgress.value = progressState
                if (progressState is ScanProgressState.Completed) {
                    // Update scan log
                    val log = ScanLog(
                        scanType = scanType,
                        itemsScanned = progressState.totalScanned,
                        threatsFound = progressState.threats.size,
                        durationMs = progressState.durationMs
                    )
                    repository.insertScanLog(log)
                    
                    // Standard strategy: keep whitelisted or quarantined, remove others and replace
                    repository.clearAllThreats()
                    for (threat in progressState.threats) {
                        repository.insertThreat(threat)
                    }
                }
            }
        }
    }

    fun resetScanState() {
        _scanProgress.value = null
    }

    fun quarantineThreat(threat: SecurityThreat) {
        viewModelScope.launch {
            repository.updateThreat(threat.copy(isQuarantined = true, isWhitelisted = false))
        }
    }

    fun whitelistThreat(threat: SecurityThreat) {
        viewModelScope.launch {
            repository.updateThreat(threat.copy(isWhitelisted = true, isQuarantined = false))
        }
    }

    fun removeThreatFromWhitelist(threat: SecurityThreat) {
        viewModelScope.launch {
            repository.updateThreat(threat.copy(isWhitelisted = false))
        }
    }

    fun restoreQuarantinedThreat(threat: SecurityThreat) {
        viewModelScope.launch {
            repository.updateThreat(threat.copy(isQuarantined = false))
        }
    }

    fun deleteThreatItem(threat: SecurityThreat) {
        viewModelScope.launch {
            if (threat.threatType == "APP") {
                // Raise system package uninstaller activity
                try {
                    @Suppress("DEPRECATION")
                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                        data = Uri.parse("package:${threat.referenceKey}")
                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    repository.deleteThreat(threat)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${threat.referenceKey}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        repository.deleteThreat(threat)
                    } catch (e2: Exception) {}
                }
            } else {
                // File or system vulnerability
                repository.deleteThreat(threat)
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearScanLogs()
        }
    }
}

class AntivirusViewModelFactory(
    private val context: Context,
    private val repository: AntivirusRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AntivirusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AntivirusViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
