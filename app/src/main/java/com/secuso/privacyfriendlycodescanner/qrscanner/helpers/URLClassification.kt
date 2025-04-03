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

package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import com.secuso.privacyfriendlycodescanner.qrscanner.R

class URLClassification(val uri: String, val baseDomain: String, val case: Case) {
    val hints = HashSet<Hint>()

    enum class Case {
        GREEN, GREY;

        fun getColorStateList(context: Context): ColorStateList {
            val typedValue = TypedValue()
            val theme = context.theme
            return when (this) {
                GREEN -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGreen, typedValue, true)
                    ColorStateList(arrayOf<IntArray>(intArrayOf(android.R.attr.state_enabled)), intArrayOf(typedValue.data))
                }

                GREY -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGrey, typedValue, true)
                    ContextCompat.getColorStateList(context, typedValue.resourceId)!!
                }
            }
        }

        fun getButtonTextColorStateList(context: Context): ColorStateList {
            return when (this) {
                GREEN -> ContextCompat.getColorStateList(context, R.color.url_classification_green_case_button_text)!!
                GREY -> ContextCompat.getColorStateList(context, R.color.url_classification_grey_case_button_text)!!
            }
        }

        fun getTexts(): Array<Int> = when (this) {
            GREEN -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_part_1, R.string.url_dialog_risk_explanation_low_risk_part_2)
            GREY -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_part_1, R.string.url_dialog_risk_explanation_unknown_risk_part_2)
        }
    }

    enum class Hint {
        /**
         * Url might contain some sort of redirect
         */
        REDIRECT,

        /**
         * Url leads to a known url shortening service
         */
        SHORT_URL,

        /**
         * Base domain is on the known domains list
         */
        KNOWN_DOMAIN,

        /**
         * Host is on the known file upload hosts list
         */
        KNOWN_FILE_UPLOAD_HOST,

        /**
         * Base domain is on the trusted domains list
         */
        TRUSTED_DOMAIN,

        /**
         * Url is on the visited sites list and was visited at least a specific number of times
         */
        VISITED_URL,

        /**
         * Urls leading to the same base domain were visited at least a specific number of times
         */
        VISITED_BASE_DOMAIN,

        /**
         * No hint for this url available
         */
        NONE;

        fun getTexts(): Array<Int> = when (this) {
            REDIRECT -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_redirect_part_1, R.string.url_dialog_risk_explanation_unknown_risk_redirect_part_2)

            SHORT_URL -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_short_url_part_1, R.string.url_dialog_risk_explanation_unknown_risk_short_url_part_2)

            KNOWN_DOMAIN -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_known_domain_part_1, R.string.url_dialog_risk_explanation_low_risk_known_domain_part_2)

            KNOWN_FILE_UPLOAD_HOST -> arrayOf(
                R.string.url_dialog_risk_explanation_unknown_risk_known_file_upload_host_part_1,
                R.string.url_dialog_risk_explanation_unknown_risk_known_file_upload_host_part_2
            )

            TRUSTED_DOMAIN -> arrayOf(
                R.string.url_dialog_risk_explanation_low_risk_trusted_domain_part_1,
                R.string.url_dialog_risk_explanation_low_risk_trusted_domain_part_2
            )

            VISITED_URL -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_visited_url_part_1, R.string.url_dialog_risk_explanation_low_risk_visited_url_part_2)

            VISITED_BASE_DOMAIN -> arrayOf(
                R.string.url_dialog_risk_explanation_unknown_risk_visited_base_domain_part_1,
                R.string.url_dialog_risk_explanation_unknown_risk_visited_base_domain_part_2
            )

            NONE -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_part_1, R.string.url_dialog_risk_explanation_unknown_risk_part_2)
        }
    }

    companion object {
        private var _knownDomains: Set<String> = setOf()
        private var _shortLinkDomains: Set<String> = setOf()
        private var _fileUploadHosts: Set<String> = setOf()
        private final val DEFAULT_NUM_VISITS_REQUIRED = 2
        private final val DEFAULT_SECONDS_TO_WAIT = 3

        private fun readListFromRaw(context: Context, @RawRes resourceID: Int): Set<String> {
            return context.resources.openRawResource(resourceID)
                .bufferedReader()
                .readLines()
                .filter { !it.startsWith("#") }
                .toSet()
        }

        fun getFileUploadHosts(context: Context): Set<String> {
            synchronized(_fileUploadHosts) {
                if (_fileUploadHosts.isEmpty()) {
                    _fileUploadHosts = readListFromRaw(context, R.raw.file_upload_hosts)
                }
            }
            return _fileUploadHosts
        }

        fun getShortLinkDomains(context: Context): Set<String> {
            synchronized(_shortLinkDomains) {
                if (_shortLinkDomains.isEmpty()) {
                    _shortLinkDomains = readListFromRaw(context, R.raw.short_link_domains)
                }
            }
            return _shortLinkDomains
        }

        fun getKnownDomains(context: Context): Set<String> {
            synchronized(_knownDomains) {
                if (_knownDomains.isEmpty()) {
                    _knownDomains = readListFromRaw(context, R.raw.known_domains)
                }
            }
            return _knownDomains
        }

        fun getVisitsRequired(context: Context): Int {
            val value: Int = try {
                context.getSharedPreferences(PreferenceKeys.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
                    .getString(PreferenceKeys.URL_CLASSIFICATION_MIN_VISITS_REQUIRED, "2")?.toInt() ?: DEFAULT_NUM_VISITS_REQUIRED
            } catch (e: NumberFormatException) {
                DEFAULT_NUM_VISITS_REQUIRED
            }
            return value
        }

        fun getGreyCaseWaitingTimeMillis(context: Context): Int {
            val value: Int = try {
                context.getSharedPreferences(PreferenceKeys.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
                    .getString(PreferenceKeys.URL_CLASSIFICATION_GREY_CASE_WAITING_TIME, "3")?.toInt() ?: DEFAULT_SECONDS_TO_WAIT
            } catch (e: NumberFormatException) {
                DEFAULT_SECONDS_TO_WAIT
            }
            return value * 1000
        }

        fun isUrlTrackingEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PreferenceKeys.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
                .getBoolean(PreferenceKeys.URL_CLASSIFICATION_URL_TRACKING_ENABLED, true)
        }
    }
}