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
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.TrustedDomainEntity

@Dao
interface TrustedDomainDao {
    @Insert
    suspend fun insert(trustedDomainEntity: TrustedDomainEntity)

    @Delete
    suspend fun delete(trustedDomainEntity: TrustedDomainEntity)

    @Query("SELECT * FROM trustedDomains")
    suspend fun getAll(): List<TrustedDomainEntity>

    @Query("SELECT * FROM trustedDomains ORDER BY baseDomain")
    fun getAllLiveData(): LiveData<List<TrustedDomainEntity>>

    @Query("SELECT * FROM trustedDomains WHERE baseDomain = :baseDomain")
    suspend fun findByDomain(baseDomain: String): TrustedDomainEntity?
}