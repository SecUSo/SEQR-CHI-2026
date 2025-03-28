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

package com.secuso.privacyfriendlycodescanner.qrscanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.daos.TrustedDomainDao
import com.secuso.privacyfriendlycodescanner.qrscanner.database.daos.URLDao
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.TrustedDomainEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.URLEntity

@Database(entities = [URLEntity::class, TrustedDomainEntity::class], version = URLClassificationDatabase.VERSION)
abstract class URLClassificationDatabase : RoomDatabase() {
    abstract fun urlDao(): URLDao
    abstract fun trustedDomainDao(): TrustedDomainDao

    companion object {
        const val VERSION = 1
        const val DB_NAME = "url_classification_database"

        @Volatile
        private var INSTANCE: URLClassificationDatabase? = null

        fun getDatabase(context: Context): URLClassificationDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    URLClassificationDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}