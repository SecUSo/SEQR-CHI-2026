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
import android.net.Uri
import android.text.Html
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.database.URLClassificationDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.TrustedDomainEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.URLEntity
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Case.GREEN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Case.GREY
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.KNOWN_DOMAIN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.KNOWN_FILE_UPLOAD_HOST
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.NONE
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.REDIRECT
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.SHORT_URL
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.TRUSTED_DOMAIN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.VISITED_BASE_DOMAIN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.URLClassification.Hint.VISITED_URL
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

    private val _classification = MutableLiveData<URLClassification?>()
    val classification: LiveData<URLClassification?>
        get() = _classification

    private val _urlDialogContinueButtonTimer = MutableLiveData<Long>()

    private val urlClassificationDatabase = URLClassificationDatabase.getDatabase(application)
    private val context by lazy { application.applicationContext }

    private suspend fun performClassification(urlString: String): URLClassification {
        val url = urlString.lowercase(Locale.getDefault())
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase()
        val protocol = uri.scheme?.lowercase()
        val path = uri.path?.lowercase()
        val parameters = uri.query?.lowercase()
        val fragment = uri.fragment?.lowercase()
        val baseDomain = Utils.extractBaseDomainFromURI(url).lowercase()

        val hints = HashSet<URLClassification.Hint>()
        // Check if the url might be a redirect
        if ((path != null && path.contains("http"))
            || (parameters != null && parameters.contains("http"))
            || fragment != null && fragment.contains("http")
        ) {
            hints.add(REDIRECT)
        }
        // Check if the base domain is a known link shortener service
        if (URLClassification.getShortLinkDomains(this.context).contains(baseDomain)) {
            hints.add(SHORT_URL)
        }
        // Check if the host is known file upload service
        if (URLClassification.getFileUploadHosts(this.context).contains(host)) {
            hints.add(KNOWN_FILE_UPLOAD_HOST)
        }
        // Check if the base domain is a well known domain
        if (URLClassification.getKnownDomains(this.context).contains(baseDomain)) {
            hints.add(KNOWN_DOMAIN)
        }
        // Check if the base domain is in the user-managed trusted domains database
        if (urlClassificationDatabase.trustedDomainDao().findByDomain(baseDomain) != null) {
            hints.add(TRUSTED_DOMAIN)
        }
        // Check if the url was visited multiple times before
        if (urlClassificationDatabase.urlDao().findByURL(url) != null
            && urlClassificationDatabase.urlDao().findByURL(url)!!.visits >= URLClassification.getVisitsRequired(this.context)
        ) {
            hints.add(VISITED_URL)
        }
        // Check if the base domain was visited multiple times before
        if (urlClassificationDatabase.urlDao().getTotalVisitsByBaseDomain(baseDomain) >= URLClassification.getVisitsRequired(this.context)
        ) {
            hints.add(VISITED_BASE_DOMAIN)
        }

        val classification = URLClassification(url, baseDomain, getFinalClassification(hints).first)
        classification.hints.addAll(hints)
        return classification
    }

    private fun getFinalClassification(hints: Set<URLClassification.Hint>): Pair<URLClassification.Case, URLClassification.Hint> {
        return if (hints.contains(REDIRECT)) {
            GREY to REDIRECT
        } else if (hints.contains(SHORT_URL)) {
            GREY to SHORT_URL
        } else if (hints.contains(KNOWN_DOMAIN)) {
            if (hints.contains(KNOWN_FILE_UPLOAD_HOST)) {
                if (hints.contains(VISITED_URL)) {
                    // Exact url was visited before
                    GREEN to VISITED_URL
                } else {
                    GREY to KNOWN_FILE_UPLOAD_HOST
                }
            } else {
                GREEN to KNOWN_DOMAIN
            }
        } else if (hints.contains(TRUSTED_DOMAIN)) {
            GREEN to TRUSTED_DOMAIN
        } else if (hints.contains(VISITED_URL)) {
            GREEN to VISITED_URL
        } else if (hints.contains(VISITED_BASE_DOMAIN)) {
            GREY to VISITED_BASE_DOMAIN
        } else {
            GREY to NONE
        }
    }

    private fun canBeAddedToTrustedDomains(hints: Set<URLClassification.Hint>): Boolean {
        return (hints.contains(VISITED_BASE_DOMAIN) && !hints.contains(TRUSTED_DOMAIN) && !hints.contains(KNOWN_DOMAIN) && !hints.contains(REDIRECT) && !hints.contains(
            SHORT_URL
        ) && !hints.contains(KNOWN_FILE_UPLOAD_HOST))
    }

    private fun addToTrustedDomains(classification: URLClassification) {
        viewModelScope.launch {
            var dbEntry = urlClassificationDatabase.trustedDomainDao().findByDomain(classification.baseDomain)
            if (dbEntry == null) {
                dbEntry = TrustedDomainEntity(0, classification.baseDomain)
                urlClassificationDatabase.trustedDomainDao().insert(dbEntry)
            }
        }
    }

    private fun removeFromTrustedDomains(classification: URLClassification) {
        viewModelScope.launch {
            val dbEntry = urlClassificationDatabase.trustedDomainDao().findByDomain(classification.baseDomain)
            if (dbEntry != null) {
                urlClassificationDatabase.trustedDomainDao().delete(dbEntry)
            }
        }
    }

    private fun createAddToTrustedDomainsDialog(classification: URLClassification, checkBox: CheckBox, activity: Activity): MaterialAlertDialogBuilder {
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(activity)
            .setMessage(Html.fromHtml(activity.getText(R.string.url_dialog_add_to_trusted_domains_dialog_text).toSpanned().toHtml().format(classification.baseDomain)))
            .setTitle(activity.getString(R.string.url_dialog_add_to_trusted_domains_dialog_title, classification.baseDomain))
            .setIcon(R.drawable.ic_baseline_public_24dp)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ -> checkBox.isChecked = false }
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            addToTrustedDomains(classification)
        }
        return builder
    }

    private fun createUrlDetailsDialog(classification: URLClassification, view: View, activity: Activity): MaterialAlertDialogBuilder {
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(activity)
            .setMessage(Utils.getHostHighlightingURI(classification.uri, view.context))
            .setTitle(R.string.full_url_dialog_title)
            .setIcon(R.drawable.ic_baseline_public_24dp)
            .setCancelable(true)
            .setNegativeButton(R.string.okay, null)
        builder.setPositiveButton(R.string.copy_to_clipboard) { _, _ ->
            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText(
                    "Text",
                    classification.uri
                )
            )
            Toast.makeText(activity, R.string.content_copied, Toast.LENGTH_SHORT).show()
        }
        return builder
    }

    fun initURLDialog(uri: String) {
        _classification.value = null
        viewModelScope.launch {
            this@URLDialogViewModel._classification.postValue(performClassification(uri))
        }
    }

    fun incrementVisits(classification: URLClassification) {
        if (classification.case == GREEN) {
            return
        }
        viewModelScope.launch {
            val url = classification.uri
            if (classification.case == GREY) {
                var dbEntry = urlClassificationDatabase.urlDao().findByURL(url)
                if (dbEntry == null) {
                    dbEntry = URLEntity(0, url, classification.baseDomain, 0)
                    urlClassificationDatabase.urlDao().insert(dbEntry)
                    dbEntry = urlClassificationDatabase.urlDao().findByURL(url)
                }
                urlClassificationDatabase.urlDao().incrementVisits(dbEntry!!.id)
            }
        }
    }

    fun initContinueButton(classification: URLClassification) {
        if (classification.case == GREEN
        ) {
            _urlDialogContinueButtonTimer.value = System.currentTimeMillis()
        } else {
            _urlDialogContinueButtonTimer.value = System.currentTimeMillis() + URLClassification.getGreyCaseWaitingTimeMillis(context)
        }
        viewModelScope.launch {
            urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            while ((_urlDialogContinueButtonTimer.value
                    ?: (System.currentTimeMillis() + URLClassification.getGreyCaseWaitingTimeMillis(context))) > System.currentTimeMillis()
            ) {
                delay(100)
                urlDialogContinueButtonPeriodicTrigger.postValue(Unit)
            }
        }
    }

    fun setupURLDialogView(urlDialog: View, classification: URLClassification, activity: Activity) {
        // Set the color based on the classification
        val colorStateList: ColorStateList = classification.case.getColorStateList(urlDialog.context)
        urlDialog.findViewById<ImageView>(R.id.dialog_border).imageTintList = colorStateList
        urlDialog.findViewById<View>(R.id.url_dialog_continue_button).setBackgroundTintList(colorStateList)
        urlDialog.findViewById<Button>(R.id.url_dialog_continue_button).setTextColor(classification.case.getButtonTextColorStateList(urlDialog.context))

        // Set the texts based on the classification
        val (_, mainHint) = getFinalClassification(classification.hints)
        urlDialog.findViewById<TextView>(R.id.url_dialog_risk_explanation_part_1).setText(mainHint.getTexts()[0])
        urlDialog.findViewById<TextView>(R.id.url_dialog_risk_explanation_part_2).setText(mainHint.getTexts()[1])

        // Add option to add domain to trusted domains
        val trustedDomainsCheckbox = urlDialog.findViewById<CheckBox>(R.id.url_dialog_trust_domain_checkbox)
        if (canBeAddedToTrustedDomains(classification.hints)) {
            trustedDomainsCheckbox.setOnCheckedChangeListener(null)
            trustedDomainsCheckbox.visibility = VISIBLE
            trustedDomainsCheckbox.isChecked = false
            trustedDomainsCheckbox.text = urlDialog.context.getString(R.string.url_dialog_add_to_trusted_domains, classification.baseDomain)
            trustedDomainsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    createAddToTrustedDomainsDialog(classification, trustedDomainsCheckbox, activity).show()
                } else {
                    removeFromTrustedDomains(classification)
                }
            }
        } else {
            trustedDomainsCheckbox.visibility = GONE
        }

        // Set the dialog to be shown when clicking on the info button
        val domainTextView: Button = urlDialog.findViewById<Button>(R.id.url_dialog_domain)
        domainTextView.text = classification.baseDomain
        domainTextView.setOnClickListener { view: View ->
            createUrlDetailsDialog(classification, view, activity).show()
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