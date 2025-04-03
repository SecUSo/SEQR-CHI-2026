package com.secuso.privacyfriendlycodescanner.qrscanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.secuso.privacyfriendlycodescanner.qrscanner.database.URLClassificationDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.TrustedDomainEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.URLEntity
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val urlClassificationDatabase = URLClassificationDatabase.getDatabase(application)
    private val context by lazy { application.applicationContext }

    val urlEntities = MediatorLiveData<List<URLEntity>>()
    val domainEntities = MediatorLiveData<List<TrustedDomainEntity>>()

    init {
        viewModelScope.launch {
            urlEntities.addSource(urlClassificationDatabase.urlDao().getAllLiveData(), urlEntities::setValue)
            domainEntities.addSource(urlClassificationDatabase.trustedDomainDao().getAllLiveData(), domainEntities::setValue)
        }
    }

    fun deleteURLEntity(id: Int) {
        viewModelScope.launch { urlClassificationDatabase.urlDao().delete(URLEntity(id, "", "", 0)) }
    }

    fun deleteDomainEntity(id: Int) {
        viewModelScope.launch { urlClassificationDatabase.trustedDomainDao().delete(TrustedDomainEntity(id, "")) }
    }
}