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

package com.secuso.privacyfriendlycodescanner.qrscanner.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.Result
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.database.URLClassificationDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.TrustedDomainEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.URLEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREEN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_PHONE
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_TEXT
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_UNKNOWN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.RED
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Companion.getClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.Utils
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil


class URLDialogViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Timestamp after which the button should be active
     */
    val urlDialogContinueButtonTimer: LiveData<Long>
        get() = _urlDialogContinueButtonTimer
    val urlDialogContinueButtonPeriodicTrigger = MutableLiveData<Unit>()

    private val _classification = MutableLiveData<QRClassification?>()
    val classification: LiveData<QRClassification?>
        get() = _classification

    private val _urlDialogContinueButtonTimer = MutableLiveData<Long>()

    private val urlClassificationDatabase = URLClassificationDatabase.getDatabase(application)
    private val context by lazy { application.applicationContext }

    private fun addToTrustedDomains(classification: QRClassification) {
        viewModelScope.launch {
            var dbEntry = urlClassificationDatabase.trustedDomainDao().findByDomain(classification.shortText)
            if (dbEntry == null) {
                dbEntry = TrustedDomainEntity(0, classification.shortText)
                urlClassificationDatabase.trustedDomainDao().insert(dbEntry)
            }
        }
    }

    private fun createUrlDetailsDialog(classification: QRClassification, view: View, activity: Activity): MaterialAlertDialogBuilder {
        val message = if (classification.case in listOf(RED, GREY_UNKNOWN, GREEN)) {
            Utils.getHostHighlightingURI(classification.text, view.context)
        } else {
            classification.text
        }
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(activity, R.style.AppTheme_CustomMaterialDialog)
            .setMessage(message)
            .setTitle(R.string.full_url_dialog_title)
            .setCancelable(true)
            .setNegativeButton(R.string.close, null)
        if (classification.case == RED) {
            builder.setPositiveButton(R.string.full_url_dialog_copy_high_risk) { _, _ ->
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(
                        "Text",
                        classification.text
                    )
                )
                Toast.makeText(activity, R.string.content_copied, Toast.LENGTH_SHORT).show()
            }
        } else {
            builder.setPositiveButton(R.string.full_url_dialog_copy) { _, _ ->
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(
                        "Text",
                        classification.text
                    )
                )
                Toast.makeText(activity, R.string.content_copied, Toast.LENGTH_SHORT).show()
            }
        }
        return builder
    }

    fun initURLDialog(rawResult: Result) {
        _classification.value = null
        viewModelScope.launch(Dispatchers.IO) {
            this@URLDialogViewModel._classification.postValue(getClassification(rawResult, context, urlClassificationDatabase))
        }
    }

    fun incrementVisits(classification: QRClassification) {
        if (classification.case == GREEN || !QRClassification.isUrlTrackingEnabled(context)) {
            return
        }
        viewModelScope.launch {
            // We only use the short text to identify the base domain
            val url = classification.shortText
            if (classification.case == GREY_UNKNOWN) {
                var dbEntry = urlClassificationDatabase.urlDao().findByURL(url)
                if (dbEntry == null) {
                    dbEntry = URLEntity(0, url, classification.shortText, 0)
                    urlClassificationDatabase.urlDao().insert(dbEntry)
                    dbEntry = urlClassificationDatabase.urlDao().findByURL(url)
                }
                urlClassificationDatabase.urlDao().incrementVisits(dbEntry!!.id)
            }
        }
    }

    fun initContinueButton(classification: QRClassification) {
        _urlDialogContinueButtonTimer.value = when (classification.case) {
            GREEN, RED -> {
                System.currentTimeMillis()
            }

            else -> {
                System.currentTimeMillis() + QRClassification.getGreyCaseWaitingTimeMillis(context)
            }
        }
        viewModelScope.launch {
            urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            while ((_urlDialogContinueButtonTimer.value
                    ?: (System.currentTimeMillis() + QRClassification.getGreyCaseWaitingTimeMillis(context))) > System.currentTimeMillis()
            ) {
                delay(100)
                urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            }
        }
    }

    fun setupURLDialogView(urlDialog: View, classification: QRClassification, activity: Activity) {
        // Set the color based on the classification
        val colorStateList: ColorStateList = classification.case.getColorStateList(urlDialog.context)
        urlDialog.findViewById<ImageView>(R.id.dialog_border).imageTintList = colorStateList
        urlDialog.findViewById<View>(R.id.url_dialog_continue_button).setBackgroundTintList(colorStateList)
        urlDialog.findViewById<Button>(R.id.url_dialog_continue_button).setTextColor(classification.case.getButtonTextColorStateList(urlDialog.context))

        // Set the texts based on the classification
        urlDialog.findViewById<TextView>(R.id.url_dialog_risk_explanation_part_1).setText(classification.case.getTexts()[0])
        urlDialog.findViewById<TextView>(R.id.url_dialog_risk_explanation_part_2).setText(classification.case.getTexts()[1])

        // Set the dialog to be shown when clicking on the info button
        val domainTextView: Button = urlDialog.findViewById<Button>(R.id.url_dialog_domain)
        domainTextView.text = when (classification.case) {
            GREEN, RED, GREY_UNKNOWN -> {
                classification.shortText
            }

            GREY_TEXT -> {
                if (classification.text.length < 100) classification.text else classification.text.substring(0, 100) + "..."
            }

            GREY_PHONE -> {
                getPhoneNumberText(classification)
            }
        }
        domainTextView.setOnClickListener { view: View ->
            val dialog = createUrlDetailsDialog(classification, view, activity).show()
            dialog.window?.findViewById<TextView>(android.R.id.message)?.setTypeface(ResourcesCompat.getFont(context, R.font.lexend))
        }
    }

    private fun getPhoneNumberText(classification: QRClassification): String {
        val phoneNumber = classification.text
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val numberProto = phoneUtil.parse(phoneNumber, null)
        return "+" + numberProto.countryCode + "(${classification.shortText})\n" + numberProto.nationalNumber
    }

    private fun getButtonText(classification: QRClassification, timeRemaining: Int, activity: Activity): String {
        return when (classification.case) {
            GREEN, GREY_UNKNOWN -> if (timeRemaining > 0) {
                activity.getString(R.string.url_dialog_website_url_open_time, timeRemaining)
            } else {
                activity.getString(R.string.url_dialog_continue_button_url)
            }

            RED -> activity.getString(R.string.close)
            GREY_TEXT -> if (timeRemaining > 0) {
                activity.getString(R.string.url_dialog_text_open_time, timeRemaining)
            } else {
                activity.getString(R.string.url_dialog_continue_button_text)
            }

            GREY_PHONE -> if (timeRemaining > 0) {
                activity.getString(R.string.url_dialog_phone_open_time, timeRemaining)
            } else {
                activity.getString(R.string.url_dialog_continue_button_phone)
            }
        }
    }

    fun updateURLDialogContinueButton(classification: QRClassification, continueButton: Button, onClickListener: View.OnClickListener, activity: Activity) {
        val enableTime: Long = urlDialogContinueButtonTimer.getValue() ?: return
        val timeRemaining = ceil(((enableTime - System.currentTimeMillis()) / 1000f).toDouble()).toInt()

        continueButton.text = getButtonText(classification, timeRemaining, activity)
        if (timeRemaining > 0) {
            continueButton.setOnClickListener { }
            continueButton.isEnabled = false
        } else {
            continueButton.setOnClickListener(onClickListener)
            continueButton.isEnabled = true
        }
    }
}