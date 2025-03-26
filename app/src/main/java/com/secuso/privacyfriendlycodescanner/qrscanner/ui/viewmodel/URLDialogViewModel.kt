package com.secuso.privacyfriendlycodescanner.qrscanner.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.database.HostsDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.HostEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.HostClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil

class URLDialogViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Timestamp after which the button should be active
     */
    val urlDialogContinueButtonTimer: LiveData<Long>
        get() = _urlDialogContinueButtonTimer
    val urlDialogContinueButtonPeriodicTrigger = MutableLiveData<Unit>()

    companion object {
        const val BLUE_CASE_VISITS_REQUIRED = 1
        const val GRAY_CASE_WAITING_TIME = 3_000 //Time in millis
    }

    private val _classification = MutableLiveData<HostClassification?>()
    val classification: LiveData<HostClassification?>
        get() = _classification

    private val _urlDialogContinueButtonTimer = MutableLiveData<Long>()
    private val hostsDatabase = HostsDatabase.getDatabase(application)
    private val context by lazy { application.applicationContext }

    private var _trustedHostsList: Set<String> = setOf()

    private suspend fun trustedHostsList(): Set<String> {
        if (_trustedHostsList.isEmpty()) {
            _trustedHostsList = readHostsFromRaw()
        }
        return _trustedHostsList
    }

    private suspend fun readHostsFromRaw(): Set<String> {
        return context.resources.openRawResource(R.raw.trusted_hosts)
            .bufferedReader()
            .readLines()
            .filter { !it.startsWith("#") }
            .toSet()
    }

    fun initURLDialog(uri: String) {
        _classification.value = null
        viewModelScope.launch {
            val baseDomain = Utils.extractBaseDomainFromURI(uri)
            val classification =
                if (trustedHostsList().contains(baseDomain.lowercase())) {
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

    fun setupURLDialogView(urlDialog: View, classification: HostClassification, activity: Activity) {
        // Set the color based on the classification
        val colorStateList: ColorStateList = classification.case.getColorStateList(urlDialog.context)
        (urlDialog.findViewById<View>(R.id.dialog_border) as ImageView).imageTintList = colorStateList
        urlDialog.findViewById<View>(R.id.url_dialog_continue_button).setBackgroundTintList(colorStateList)
        (urlDialog.findViewById<View>(R.id.url_dialog_continue_button) as Button).setTextColor(classification.case.getButtonTextColorStateList(urlDialog.context))

        // Set the texts based on the classification
        (urlDialog.findViewById<View>(R.id.url_dialog_risk_explanation_part_1) as TextView).setText(classification.case.getTexts().get(0))
        (urlDialog.findViewById<View>(R.id.url_dialog_risk_explanation_part_2) as TextView).setText(classification.case.getTexts().get(1))


        // Set the dialog to be shown when clicking on the info button
        val domainTextView: Button = urlDialog.findViewById<Button>(R.id.url_dialog_domain)
        domainTextView.text = Utils.extractBaseDomainFromURI(classification.uri.lowercase(Locale.getDefault()))
        domainTextView.setOnClickListener { view: View ->
            val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(activity)
                .setMessage(Utils.getHostHighlightingURI(classification.uri, view.context))
                .setTitle(R.string.full_url_dialog_title)
                .setIcon(R.drawable.ic_baseline_public_24dp)
                .setCancelable(true)
                .setNegativeButton(R.string.okay, null)
            builder.setPositiveButton(R.string.copy_to_clipboard) { dialog, which ->
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(
                        "Text",
                        classification.uri
                    )
                )
                Toast.makeText(activity, R.string.content_copied, Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }
    }

    fun updateURLDialogContinueButton(continueButton: Button, onClickListener: View.OnClickListener, activity: Activity) {
        val enableTime: Long = urlDialogContinueButtonTimer.getValue() ?: return
        val timeRemaining = ceil(((enableTime - System.currentTimeMillis()) / 1000f).toDouble()).toInt()

        if (timeRemaining > 0) {
            continueButton.text = activity.getString(R.string.url_dialog_website_open_time, timeRemaining)
            continueButton.setOnClickListener { }
            continueButton.isEnabled = false
        } else {
            continueButton.setText(R.string.url_dialog_continue_button)
            continueButton.setOnClickListener(onClickListener)
            continueButton.isEnabled = true
        }
    }
}