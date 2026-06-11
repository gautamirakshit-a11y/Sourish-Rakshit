package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_logs")
data class ScanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String, // "QUICK", "FULL", "REAL_TIME"
    val itemsScanned: Int,
    val threatsFound: Int,
    val durationMs: Long
)

@Entity(tableName = "security_threats")
data class SecurityThreat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val threatType: String, // "APP", "FILE", "VULNERABILITY"
    val severity: String, // "CRITICAL", "MEDIUM", "LOW"
    val description: String,
    val referenceKey: String, // package name, file path, or vulnerability ID
    val isQuarantined: Boolean = false,
    val isWhitelisted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AntivirusDao {
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getScanLogsFlow(): Flow<List<ScanLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanLog(log: ScanLog): Long

    @Query("SELECT * FROM security_threats ORDER BY timestamp DESC")
    fun getThreatsFlow(): Flow<List<SecurityThreat>>

    @Query("SELECT * FROM security_threats WHERE isWhitelisted = 0 AND isQuarantined = 0 ORDER BY timestamp DESC")
    fun getActiveThreatsFlow(): Flow<List<SecurityThreat>>

    @Query("SELECT * FROM security_threats WHERE isQuarantined = 1 ORDER BY timestamp DESC")
    fun getQuarantinedThreatsFlow(): Flow<List<SecurityThreat>>

    @Query("SELECT * FROM security_threats WHERE isWhitelisted = 1 ORDER BY timestamp DESC")
    fun getWhitelistedThreatsFlow(): Flow<List<SecurityThreat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: SecurityThreat): Long

    @Update
    suspend fun updateThreat(threat: SecurityThreat)

    @Delete
    suspend fun deleteThreat(threat: SecurityThreat)

    @Query("DELETE FROM security_threats WHERE referenceKey = :refKey")
    suspend fun deleteThreatByReferenceKey(refKey: String)

    @Query("DELETE FROM security_threats")
    suspend fun clearAllThreats()

    @Query("DELETE FROM scan_logs")
    suspend fun clearScanLogs()
}

@Database(entities = [ScanLog::class, SecurityThreat::class], version = 1, exportSchema = false)
abstract class AntivirusDatabase : RoomDatabase() {
    abstract fun antivirusDao(): AntivirusDao

    companion object {
        @Volatile
        private var INSTANCE: AntivirusDatabase? = null

        fun getDatabase(context: Context): AntivirusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AntivirusDatabase::class.java,
                    "antivirus_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
