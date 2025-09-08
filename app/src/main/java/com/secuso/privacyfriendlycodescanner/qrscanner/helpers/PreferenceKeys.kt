/*
    Privacy Friendly QR Scanner
    Copyright (C) 2023-2025 Privacy Friendly QR Scanner authors and SECUSO

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

object PreferenceKeys {
    const val SEARCH_ENGINE = "pref_search_engine"
    const val APP_THEME = "pref_app_theme"
    const val APP_VERSION_LAST_OPENED = "pref_app_version_last_opened"
    const val URL_CLASSIFICATION_GREY_CASE_WAITING_TIME = "pref_url_classification_grey_case_waiting_time"
    const val URL_CLASSIFICATION_URL_TRACKING_ENABLED = "pref_url_classification_url_tracking_enabled"
    const val PHISHING_PROTECTION_PHISHTANK_ENABLED = "pref_phishing_protection_phishtank_enabled"
    const val PHISHING_PROTECTION_RESOLVE_REDIRECTS_ENABLED = "pref_phishing_protection_resolve_redirects_enabled"

    @JvmStatic
    fun getDefaultSharedPreferencesName(context: Context): String {
        return context.packageName + "_preferences"
    }
}