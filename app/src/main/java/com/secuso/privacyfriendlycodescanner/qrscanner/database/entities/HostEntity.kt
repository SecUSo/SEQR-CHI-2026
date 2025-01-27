package com.secuso.privacyfriendlycodescanner.qrscanner.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val host: String,
    val visits: Int
)
