package com.secuso.privacyfriendlycodescanner.qrscanner.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.HostEntity

@Dao
interface HostDao {
    @Insert
    suspend fun insert(hostEntity: HostEntity)

    @Delete
    suspend fun delete(hostEntity: HostEntity)

    @Query("SELECT * FROM hosts")
    suspend fun getAllHosts(): List<HostEntity>

    @Query("UPDATE hosts SET visits = :visits WHERE id = :id")
    suspend fun updateVisits(id: Int, visits: Int)

    @Query("SELECT * FROM hosts WHERE host = :host")
    suspend fun getHost(host: String): HostEntity?

    @Query("UPDATE hosts SET visits = visits + 1 WHERE id = :id")
    suspend fun incrementVisits(id: Int)
}