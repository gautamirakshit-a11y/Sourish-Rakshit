package com.example.engine

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

data class AppSignatureDetails(
    val label: String,
    val packageName: String,
    val signatureHash: String,
    val isSystem: Boolean,
    val isMaliciousMatch: Boolean,
    val detectionInfo: String?,
    val description: String? = null
)

object SignatureDatabase {
    // Unknown or malicious blacklisted developers signature database (SHA-256 hashes)
    val maliciousSignatures = mapOf(
        "A1B2C3D4E5F678901234567890ABCDEF1234567890ABCDEF1234567890ABCDEF" to Pair(
            "Trojan.Android.Cerberus",
            "Critical banker Trojan signature match. Intercepts outgoing SMS verification and pushes phishing overlays onto financial applications."
        ),
        "9876543210FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210FEDCBA" to Pair(
            "Adware.Fleeceware",
            "Premium-toll billing fraud signature match. Silently triggers background user subscriptions to non-approved shortcodes."
        ),
        "3E59B3C73CE8B0BDBF6D93EBC03EAC1BFCE4579DFE7B9BEB577A7799CA81992F" to Pair(
            "Backdoor.Android.SpyG",
            "Remote access spyware payload signature. Frequently bundled inside repackaged utility downloads to capture screens and physical taps."
        )
    )

    fun getAppSignatureHash(context: Context, packageName: String): String {
        try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // PackageManager.GET_SIGNING_CERTIFICATES is API level 28+
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    val signatures = if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                    if (signatures != null && signatures.isNotEmpty()) {
                        return hashSignature(signatures[0].toByteArray())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val signatures = packageInfo.signatures
                if (signatures != null && signatures.isNotEmpty()) {
                    return hashSignature(signatures[0].toByteArray())
                }
            }
        } catch (e: Exception) {
            // safe fallback for app environments
        }
        return ""
    }

    private fun hashSignature(signatureBytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatureBytes)
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
