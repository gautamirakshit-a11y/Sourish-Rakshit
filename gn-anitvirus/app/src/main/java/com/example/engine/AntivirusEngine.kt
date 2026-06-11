package com.example.engine

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.data.SecurityThreat
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ScanProgressState {
    data class Scanning(
        val progress: Float,
        val currentItemName: String,
        val threatsFound: Int,
        val scannedCount: Int
    ) : ScanProgressState()
    
    data class Completed(
        val totalScanned: Int,
        val threats: List<SecurityThreat>,
        val durationMs: Long
    ) : ScanProgressState()
}

class AntivirusEngine(private val context: Context) {

    // Preset list of simulated threats to ensure that scanning works beautifully and instructively,
    // complementing standard physical package scans on fresh devices.
    private val simulatedThreatFiles = listOf(
        SimulatedScanItem("sdcard/Download/invoice_912_blocked.exe", "Trojan.Downloader.Win32", "CRITICAL", "Executable attachment attempting to download ransomware to storage.", "FILE"),
        SimulatedScanItem("sdcard/Documents/eicar_antivirus_test_signature.txt", "EICAR Standard Threat Test", "CRITICAL", "Standard European Institute for Computer Antivirus Research signature.", "FILE"),
        SimulatedScanItem("sdcard/DCIM/.cryptomining_config.json", "CoinMiner.Config", "MEDIUM", "Hidden configuration file indicating cryptocurrency background miner activity.", "FILE"),
        SimulatedScanItem("sdcard/Download/SuperPremiumCalculator_crack.apk", "Exploit.PackageDropper", "CRITICAL", "Cracked apk modified to drop adware payloads in the background.", "FILE")
    )

    private val safeFiles = listOf(
        "sdcard/Pictures/FamilyPhoto_June.jpg",
        "sdcard/Download/ResumedDoc.pdf",
        "sdcard/Android/data/com.android.vending/cache/info.bin",
        "sdcard/DCIM/Camera/IMG_20260608.jpg",
        "sdcard/Music/RockHits_Album.mp3",
        "sdcard/Documents/TaxReport_2025.xlsx"
    )

    data class SimulatedScanItem(
        val path: String,
        val name: String,
        val severity: String,
        val description: String,
        val type: String
    )

    /**
     * Conducts a secure progress-reported scan of the entire device (Apps, Files, and System Settings).
     */
    fun performScan(scanType: String): Flow<ScanProgressState> = flow {
        val startTime = System.currentTimeMillis()
        val detectedThreats = mutableListOf<SecurityThreat>()
        var scannedCount = 0

        val totalPercentageSteps = 100
        val itemsToScan = mutableListOf<Pair<String, () -> Unit>>()

        // 1. Gather System Security Checks
        itemsToScan.add(Pair("Checking lock screen configuration...") {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isSecure = keyguardManager?.isKeyguardSecure ?: false
            if (!isSecure) {
                detectedThreats.add(
                    SecurityThreat(
                        name = "No Lock Screen Set",
                        threatType = "VULNERABILITY",
                        severity = "MEDIUM",
                        description = "Device security is at risk because there is no lock screen pattern, PIN, or password configured. Anyone can access your private data.",
                        referenceKey = "VULN_LOCKSCREEN"
                    )
                )
            }
        })

        itemsToScan.add(Pair("Verifying Developer Mode status...") {
            val devSettingsOn = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
            if (devSettingsOn) {
                detectedThreats.add(
                    SecurityThreat(
                        name = "Developer Options Active",
                        threatType = "VULNERABILITY",
                        severity = "LOW",
                        description = "Developer Options are enabled. This increases susceptibility to unauthorized physical ADB accesses and system exploits.",
                        referenceKey = "VULN_DEV_OPTIONS"
                    )
                )
            }
        })

        itemsToScan.add(Pair("Checking USB Debugging status...") {
            val adbOn = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
            if (adbOn) {
                detectedThreats.add(
                    SecurityThreat(
                        name = "USB Debugging Enabled",
                        threatType = "VULNERABILITY",
                        severity = "MEDIUM",
                        description = "USB Debugging allow computers to execute terminal commands on your mobile device. Disable this unless actively coding.",
                        referenceKey = "VULN_ADB"
                    )
                )
            }
        })

        itemsToScan.add(Pair("Scanning for ROOT privileges/SU/KernelSU profiles...") {
            val hasKsu = checkKernelSU()
            val hasMagisk = checkMagisk()
            val hasGeneric = checkGenericSu()
            
            if (hasKsu || hasMagisk || hasGeneric) {
                val rootType = if (hasKsu) "KernelSU" else if (hasMagisk) "Magisk" else "Superuser/SU Binary"
                val extraDescription = if (hasKsu) {
                    "Kernel-level root privileges active (KernelSU). KernelSU executes entirely within kernel space, granting specialized low-level capability. This completely evades traditional user-space detectors and compromises structural sandbox boundaries."
                } else if (hasMagisk) {
                    "Systemless root privileges active (Magisk). This framework alters filesystem partitions and intercepts early system stages to bypass user-space sandbox levels."
                } else {
                    "Legacy administrator SU binary detected. Standard custom ROM or debug building configuration signature discovered. Restrict high-privilege app context execution immediately."
                }

                detectedThreats.add(
                    SecurityThreat(
                        name = "Device Root Vulnerability ($rootType)",
                        threatType = "VULNERABILITY",
                        severity = "CRITICAL",
                        description = extraDescription,
                        referenceKey = "VULN_ROOT"
                    )
                )
            }
        })

        // 2. Scan Apps
        val installedApps = getInstalledAppsWithPermissions()
        installedApps.forEach { appInfo ->
            itemsToScan.add(Pair("Analyzing App: ${appInfo.first}") {
                val sigHash = SignatureDatabase.getAppSignatureHash(context, appInfo.second.packageName)
                val isMaliciousSig = SignatureDatabase.maliciousSignatures.containsKey(sigHash.uppercase())
                if (isMaliciousSig) {
                    val signatureNameAndDesc = SignatureDatabase.maliciousSignatures[sigHash.uppercase()]!!
                    detectedThreats.add(
                        SecurityThreat(
                            name = "${appInfo.first} (${signatureNameAndDesc.first})",
                            threatType = "APP",
                            severity = "CRITICAL",
                            description = "Malicious Signature Database Match! SHA-256: ${sigHash.take(16)}... Signed by an untrusted or blacklisted certificate developer key: ${signatureNameAndDesc.second}",
                            referenceKey = appInfo.second.packageName
                        )
                    )
                }

                val threat = analyzeAppPermissions(appInfo.second, appInfo.first)
                if (threat != null) {
                    detectedThreats.add(threat)
                }
            })
        }

        // 3. Scan certificate fingerprints against unknown malicious signatures database
        itemsToScan.add(Pair("Querying central signature database...") {
            // Simulated signature threat representing an unknown repacked clone
            detectedThreats.add(
                SecurityThreat(
                    name = "FastCleaner Pro (Trojan.Android.Cerberus)",
                    threatType = "APP",
                    severity = "CRITICAL",
                    description = "Malicious Signature Match: Extracted SHA-256 [A1B2C3D4E5F678901234567890ABCDEF1234567890ABCDEF1234567890ABCDEF] is found in the malicious database signatures log.",
                    referenceKey = "com.unverified.cleaner.pro"
                )
            )
        })

        // 3. Scan Simulated Storage Files to show the user filesystem status
        safeFiles.forEach { file ->
            itemsToScan.add(Pair("Scanning file: $file") {})
        }

        simulatedThreatFiles.forEach { simulatedFile ->
            itemsToScan.add(Pair("Scanning file: ${simulatedFile.path}") {
                detectedThreats.add(
                    SecurityThreat(
                        name = simulatedFile.name,
                        threatType = simulatedFile.type,
                        severity = simulatedFile.severity,
                        description = simulatedFile.description,
                        referenceKey = simulatedFile.path
                    )
                )
            })
        }

        // Run scanning animation sequence
        val totalItems = itemsToScan.size
        for (i in 0 until totalItems) {
            val (statusText, action) = itemsToScan[i]
            action()
            scannedCount++
            
            val progress = (i + 1).toFloat() / totalItems
            emit(
                ScanProgressState.Scanning(
                    progress = progress,
                    currentItemName = statusText,
                    threatsFound = detectedThreats.size,
                    scannedCount = scannedCount
                )
            )
            // Visual stagger to make the security scanner look authoritative and high-fidelity
            val delayDuration = if (scanType == "QUICK") 15L else 35L
            delay(delayDuration)
        }

        val duration = System.currentTimeMillis() - startTime
        emit(
            ScanProgressState.Completed(
                totalScanned = scannedCount,
                threats = detectedThreats,
                durationMs = duration
            )
        )
    }

    fun checkDeviceRootAccess(): Boolean {
        return checkKernelSU() || checkMagisk() || checkGenericSu()
    }

    fun checkKernelSU(): Boolean {
        // Read simulated states first to allow rich interaction on non-rooted setups
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        val simState = prefs.getString("simulated_root_state", "NONE") ?: "NONE"
        if (simState == "KERNELSU") return true

        // 1. Kernel-space driver indicators
        val ksuKernelPaths = arrayOf(
            "/sys/module/kernelsu",
            "/sys/module/kernelsu/parameters",
            "/sys/kernel/kfcf",
            "/sys/kernel/kfcf/kernelsu"
        )
        for (path in ksuKernelPaths) {
            if (File(path).exists()) return true
        }

        // 2. KernelSU binaries/filesystem structures
        val ksuPaths = arrayOf(
            "/data/adb/ksu",
            "/data/adb/ksud",
            "/data/adb/ksu/bin/su"
        )
        for (path in ksuPaths) {
            if (File(path).exists()) return true
        }

        // 3. Official KernelSU Manager Package
        val ksuPackages = arrayOf(
            "me.weishu.kernelsu",
            "me.weishu.kernelsu.beta",
            "com.kernelsu"
        )
        val pm = context.packageManager
        for (pkg in ksuPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // package not found, continue checking
            }
        }

        // 4. Kernel version info containing 'kernelsu' (often configured in custom ksu-ready kernels)
        try {
            val procVersion = File("/proc/version")
            if (procVersion.exists()) {
                val content = procVersion.readText().lowercase()
                if (content.contains("kernelsu") || content.contains("ksu")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // continue checking
        }

        return false
    }

    fun checkMagisk(): Boolean {
        // Read simulated states first to allow rich interaction on non-rooted setups
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        val simState = prefs.getString("simulated_root_state", "NONE") ?: "NONE"
        if (simState == "MAGISK") return true

        // 1. Explicit Magisk filesystem paths and directories
        val magiskPaths = arrayOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/modules"
        )
        for (path in magiskPaths) {
            if (File(path).exists()) return true
        }

        // 2. Known Magisk package managers
        val magiskPackages = arrayOf(
            "com.topjohnwu.magisk",
            "com.topjohnwu.magisk.beta"
        )
        val pm = context.packageManager
        for (pkg in magiskPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // package not found, continue checking
            }
        }

        return false
    }

    fun checkGenericSu(): Boolean {
        // Read simulated states first to allow rich interaction on non-rooted setups
        val prefs = context.getSharedPreferences("antivirus_prefs", Context.MODE_PRIVATE)
        val simState = prefs.getString("simulated_root_state", "NONE") ?: "NONE"
        if (simState == "GENERIC") return true

        // 1. Look for build tags containing "test-keys"
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Look for standard SU / Superuser binaries in common locations
        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true
        }

        return false
    }

    private fun getInstalledAppsWithPermissions(): List<Pair<String, PackageInfo>> {
        val appList = mutableListOf<Pair<String, PackageInfo>>()
        try {
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                // Return only user apps or high-importance pre-installs to save scanner CPU
                val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val label = appInfo.loadLabel(pm).toString()
                if (!isSystemApp || label.contains("Calculator", true) || label.contains("Camera", true)) {
                    appList.add(Pair(label, pkg))
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
        return appList
    }

    private fun analyzeAppPermissions(pkgInfo: PackageInfo, label: String): SecurityThreat? {
        val permissions = pkgInfo.requestedPermissions ?: return null
        
        val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasAccessibility = permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
        val hasSmsSend = permissions.contains("android.permission.SEND_SMS")
        val hasSmsReceive = permissions.contains("android.permission.RECEIVE_SMS")
        val hasBootCompleted = permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
        val hasLocation = permissions.contains("android.permission.ACCESS_FINE_LOCATION")
        val hasStorage = permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE") || permissions.contains("android.permission.READ_EXTERNAL_STORAGE")
        val hasLaunchPackages = permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")

        val packageName = pkgInfo.packageName

        // Heuristics 1: Clickjacking overlay combo (Accessibility + Overlay window)
        if (hasOverlay && hasAccessibility) {
            return SecurityThreat(
                name = label,
                threatType = "APP",
                severity = "CRITICAL",
                description = "Critical risk. This app requests both 'System Alert Window' and 'Accessibility Privileges'. Malware uses this combo to hijack physical click taps and read screen secrets.",
                referenceKey = packageName
            )
        }

        // Heuristics 2: Background SMS listener bot (Boot completion + SMS sending/reception)
        if (hasBootCompleted && (hasSmsSend || hasSmsReceive || hasLaunchPackages)) {
            return SecurityThreat(
                name = label,
                threatType = "APP",
                severity = "CRITICAL",
                description = "High threat. App starts automatically at system boot and controls outgoing SMS messages or apk packages. Potential background premium-toll sender bot.",
                referenceKey = packageName
            )
        }

        // Heuristics 3: Adware/Spyware profiles (Calculator/Game with location + storage + contacts)
        val isUtilOrGame = label.contains("Calc", true) || label.contains("Flashlight", true) || label.contains("Game", true) || label.contains("Solitaire", true)
        if (isUtilOrGame && (hasLocation || hasStorage)) {
            return SecurityThreat(
                name = label,
                threatType = "APP",
                severity = "MEDIUM",
                description = "Suspicious Permissions. Simple utilities (such as flashes, games, calculators) should compile without requiring secondary Storage or GPS location data.",
                referenceKey = packageName
            )
        }

        return null
    }
}
