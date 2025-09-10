/*
    Privacy Friendly QR Scanner
    Copyright (C) 2022-2025 Privacy Friendly QR Scanner authors and SECUSO

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

package com.secuso.privacyfriendlycodescanner.qrscanner.backup

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.JsonWriter
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.secuso.privacyfriendlycodescanner.qrscanner.database.AppDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.URLClassificationDatabase
import org.secuso.privacyfriendlybackup.api.backup.DatabaseUtil.writeDatabase
import org.secuso.privacyfriendlybackup.api.backup.PreferenceUtil.writePreferences
import org.secuso.privacyfriendlybackup.api.pfa.IBackupCreator
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.text.Charsets.UTF_8


class BackupCreator : IBackupCreator {
    private fun writeDatabase(databaseName: String, databaseVersion: Int, backupDatabaseName: String, writer: JsonWriter, context: Context) {
        val callback: SupportSQLiteOpenHelper.Callback =
            object : SupportSQLiteOpenHelper.Callback(databaseVersion) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                }
            }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(databaseName)
                .callback(callback).build()
        )

        val dataBase = helper.writableDatabase

        Log.d(TAG, "Writing database $databaseName -> $backupDatabaseName")
        writer.name(backupDatabaseName)
        writeDatabase(writer, dataBase)
        dataBase.close()
        Log.d(TAG, "Writing database $databaseName -> $backupDatabaseName complete")
    }

    override fun writeBackup(context: Context, outputStream: OutputStream): Boolean {
        Log.d(TAG, "createBackup() started")
        val outputStreamWriter = OutputStreamWriter(outputStream, UTF_8)
        val writer = JsonWriter(outputStreamWriter)
        writer.setIndent("")

        try {
            writer.beginObject()

            writeDatabase(AppDatabase.DB_NAME, AppDatabase.VERSION, backupDatabaseNameAppDatabase, writer, context)
            writeDatabase(URLClassificationDatabase.DB_NAME, URLClassificationDatabase.VERSION, backupDatabaseNameURLClassificationDatabase, writer, context)

            Log.d(TAG, "Writing preferences")
            writer.name("preferences")
            val pref: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            writePreferences(writer, pref)
            Log.d(TAG, "Writing files")
            writer.endObject()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred", e)
            e.printStackTrace()
            return false
        }

        Log.d(TAG, "Backup created successfully")
        return true
    }

    companion object {
        private const val TAG = "PFA BackupCreator"
        const val backupDatabaseNameAppDatabase = "database"
        const val backupDatabaseNameURLClassificationDatabase = "url_classification_database"
    }
}