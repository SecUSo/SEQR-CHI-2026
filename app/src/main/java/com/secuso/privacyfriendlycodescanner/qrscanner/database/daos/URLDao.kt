/*
    Privacy Friendly QR Scanner
    Copyright (C) 2025 Privacy Friendly QR Scanner authors and SECUSO

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.secuso.privacyfriendlycodescanner.qrscanner.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.URLEntity

@Dao
interface URLDao {
    @Insert
    suspend fun insert(urlEntity: URLEntity)

    @Delete
    suspend fun delete(urlEntity: URLEntity)

    @Query("SELECT * FROM visitedUrls WHERE id = :id")
    suspend fun getByID(id: Int): URLEntity

    @Query("SELECT * FROM visitedUrls")
    suspend fun getAll(): List<URLEntity>

    @Query("SELECT * FROM visitedUrls ORDER BY baseDomain,url")
    fun getAllLiveData(): LiveData<List<URLEntity>>

    @Query("UPDATE visitedUrls SET visits = :visits WHERE id = :id")
    suspend fun updateVisits(id: Int, visits: Int)

    @Query("SELECT * FROM visitedUrls WHERE url = :url")
    suspend fun findByURL(url: String): URLEntity?

    @Query("SELECT * FROM visitedUrls WHERE baseDomain = :baseDomain")
    suspend fun findByBaseDomain(baseDomain: String): List<URLEntity>

    @Query("SELECT SUM(visits) FROM visitedUrls WHERE baseDomain = :baseDomain")
    suspend fun getTotalVisitsByBaseDomain(baseDomain: String): Int

    @Query("UPDATE visitedUrls SET visits = visits + 1 WHERE id = :id")
    suspend fun incrementVisits(id: Int)

    @Query("DELETE FROM visitedUrls")
    suspend fun deleteAll()
}