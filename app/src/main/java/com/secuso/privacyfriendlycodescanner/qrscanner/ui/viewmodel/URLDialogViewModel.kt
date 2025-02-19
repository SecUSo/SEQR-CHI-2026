package com.secuso.privacyfriendlycodescanner.qrscanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.secuso.privacyfriendlycodescanner.qrscanner.database.HostsDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.HostEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.HostClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.HostClassification.Companion.BLUE_CASE_VISITS_REQUIRED
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.HostClassification.Companion.GRAY_CASE_WAITING_TIME
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.HostClassification.Companion.HOSTS_LIST
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class URLDialogViewModel(application: Application) : AndroidViewModel(application) {

    private val _classification = MutableLiveData<HostClassification?>()
    val classification: LiveData<HostClassification?>
        get() = _classification

    private val _urlDialogContinueButtonTimer = MutableLiveData<Long>()
    val urlDialogContinueButtonTimer: LiveData<Long>
        get() = _urlDialogContinueButtonTimer
    val urlDialogContinueButtonPeriodicTrigger = MutableLiveData<Unit>()

    private val hostsDatabase = HostsDatabase.getDatabase(application)
    fun initURLDialog(uri: String) {
        viewModelScope.launch {
            val baseDomain = Utils.extractBaseDomainFromURI(uri)
            val classification =
                if (HOSTS_LIST.contains(baseDomain.lowercase())) {
                    HostClassification.Case.GREEN
                } else if (hostsDatabase.hostDao().getHost(baseDomain) != null
                    && hostsDatabase.hostDao().getHost(baseDomain)!!.visits >= BLUE_CASE_VISITS_REQUIRED
                ) {
                    HostClassification.Case.BLUE
                } else {
                    HostClassification.Case.GRAY
                }
            this@URLDialogViewModel._classification.postValue(HostClassification(uri, classification))
        }
    }

    fun incrementVisits(classification: HostClassification) {
        if (classification.case == HostClassification.Case.GREEN) {
            return
        }
        viewModelScope.launch {
            val baseDomain = Utils.extractBaseDomainFromURI(classification.uri)
            if (classification.case == HostClassification.Case.GRAY || classification.case == HostClassification.Case.BLUE) {
                var hostEntity = hostsDatabase.hostDao().getHost(baseDomain)
                if (hostEntity == null) {
                    hostEntity = HostEntity(0, baseDomain, 0)
                    hostsDatabase.hostDao().insert(hostEntity)
                    hostEntity = hostsDatabase.hostDao().getHost(baseDomain)
                }
                hostsDatabase.hostDao().incrementVisits(hostEntity!!.id)
            }
        }
    }

    fun initContinueButton(classification: HostClassification) {
        if (classification.case == HostClassification.Case.GREEN
            || classification.case == HostClassification.Case.BLUE
        ) {
            _urlDialogContinueButtonTimer.value = System.currentTimeMillis()
        } else {
            _urlDialogContinueButtonTimer.value = System.currentTimeMillis() + GRAY_CASE_WAITING_TIME
        }
        viewModelScope.launch {
            urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            while ((_urlDialogContinueButtonTimer.value ?: (System.currentTimeMillis() + GRAY_CASE_WAITING_TIME)) > System.currentTimeMillis()) {
                delay(100)
                urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            }
        }
    }
}