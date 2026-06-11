package com.example.data

import kotlinx.coroutines.flow.Flow

class AntivirusRepository(private val dao: AntivirusDao) {

    val scanLogs: Flow<List<ScanLog>> = dao.getScanLogsFlow()
    val allThreats: Flow<List<SecurityThreat>> = dao.getThreatsFlow()
    val activeThreats: Flow<List<SecurityThreat>> = dao.getActiveThreatsFlow()
    val quarantinedThreats: Flow<List<SecurityThreat>> = dao.getQuarantinedThreatsFlow()
    val whitelistedThreats: Flow<List<SecurityThreat>> = dao.getWhitelistedThreatsFlow()

    suspend fun insertScanLog(log: ScanLog): Long {
        return dao.insertScanLog(log)
    }

    suspend fun insertThreat(threat: SecurityThreat): Long {
        return dao.insertThreat(threat)
    }

    suspend fun updateThreat(threat: SecurityThreat) {
        dao.updateThreat(threat)
    }

    suspend fun deleteThreat(threat: SecurityThreat) {
        dao.deleteThreat(threat)
    }

    suspend fun deleteThreatByReferenceKey(refKey: String) {
        dao.deleteThreatByReferenceKey(refKey)
    }

    suspend fun clearAllThreats() {
        dao.clearAllThreats()
    }

    suspend fun clearScanLogs() {
        dao.clearScanLogs()
    }
}
