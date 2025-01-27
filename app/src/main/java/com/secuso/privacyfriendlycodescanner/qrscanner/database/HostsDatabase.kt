package com.secuso.privacyfriendlycodescanner.qrscanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.daos.HostDao
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.HostEntity

@Database(entities = [HostEntity::class], version = 1)
abstract class HostsDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao

    companion object {
        @Volatile
        private var INSTANCE: HostsDatabase? = null

        fun getDatabase(context: Context): HostsDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HostsDatabase::class.java,
                    "hosts_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}