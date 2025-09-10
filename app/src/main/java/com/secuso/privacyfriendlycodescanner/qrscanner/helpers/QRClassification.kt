package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.util.TypedValue
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.TextParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.database.URLClassificationDatabase
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREEN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_PHONE
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_TEXT
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.GREY_UNKNOWN
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case.RED
import java.net.IDN
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class QRClassification(val text: String, val shortText: String, val case: Case, val networkError: Boolean) {
    enum class Case {
        GREEN, GREY_PHONE, GREY_TEXT, GREY_UNKNOWN, RED;

        fun getColorStateList(context: Context): ColorStateList {
            val typedValue = TypedValue()
            val theme = context.theme
            return when (this) {
                GREEN -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGreen, typedValue, true)
                    ColorStateList(arrayOf<IntArray>(intArrayOf(android.R.attr.state_enabled)), intArrayOf(typedValue.data))
                }

                GREY_PHONE, GREY_TEXT, GREY_UNKNOWN -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGrey, typedValue, true)
                    ContextCompat.getColorStateList(context, typedValue.resourceId)!!
                }

                RED -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationRed, typedValue, true)
                    ContextCompat.getColorStateList(context, typedValue.resourceId)!!
                }
            }
        }

        fun getButtonTextColorStateList(context: Context): ColorStateList {
            return when (this) {
                GREEN -> ContextCompat.getColorStateList(context, R.color.url_classification_green_case_button_text)!!
                GREY_PHONE, GREY_TEXT, GREY_UNKNOWN -> ContextCompat.getColorStateList(context, R.color.url_classification_grey_case_button_text)!!
                RED -> ContextCompat.getColorStateList(context, R.color.url_classification_red_case_button_text)!!
            }
        }

        fun getTexts(): Array<Int> = when (this) {
            GREEN -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_part_1, R.string.url_dialog_risk_explanation_low_risk_part_2)
            GREY_PHONE -> arrayOf(R.string.url_dialog_risk_explanation_phone_part_1, R.string.url_dialog_risk_explanation_phone_part_2)
            GREY_TEXT -> arrayOf(R.string.url_dialog_risk_explanation_text_part_1, R.string.url_dialog_risk_explanation_text_part_2)
            GREY_UNKNOWN -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_part_1, R.string.url_dialog_risk_explanation_unknown_risk_part_2)
            RED -> arrayOf(R.string.url_dialog_risk_explanation_high_risk_part_1, R.string.url_dialog_risk_explanation_high_risk_part_2)
        }
    }

    companion object {
        private val TAG = "QRClassification"
        private var _knownDomains: Set<String> = setOf()
        private var _shortLinkDomains: Set<String> = setOf()
        private val DEFAULT_NUM_VISITS_REQUIRED = 2
        private val DEFAULT_SECONDS_TO_WAIT = 3

        private fun readListFromRaw(context: Context, @RawRes resourceID: Int): Set<String> {
            return context.resources.openRawResource(resourceID)
                .bufferedReader()
                .readLines()
                .filter { !it.startsWith("#") }
                .toSet()
        }

        fun getShortLinkDomains(context: Context): Set<String> {
            synchronized(_shortLinkDomains) {
                if (_shortLinkDomains.isEmpty()) {
                    _shortLinkDomains = readListFromRaw(context, R.raw.short_link_domains)
                }
            }
            return _shortLinkDomains
        }

        /**
         * Do not use. Only for Unit tests.
         */
        fun setShortLinkDomains(domains: Set<String>) {
            _shortLinkDomains = domains
        }

        fun getKnownDomains(context: Context): Set<String> {
            synchronized(_knownDomains) {
                if (_knownDomains.isEmpty()) {
                    _knownDomains = readListFromRaw(context, R.raw.known_domains)
                }
            }
            return _knownDomains
        }

        /**
         * Do not use. Only for Unit tests.
         */
        fun setKnownDomains(domains: Set<String>) {
            _knownDomains = domains
        }

        fun getVisitsRequired(context: Context): Int {
            return DEFAULT_NUM_VISITS_REQUIRED
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

        fun isPhishTankEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PreferenceKeys.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
                .getBoolean(PreferenceKeys.PHISHING_PROTECTION_PHISHTANK_ENABLED, true)
        }

        fun isResolveRedirectsEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PreferenceKeys.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
                .getBoolean(PreferenceKeys.PHISHING_PROTECTION_RESOLVE_REDIRECTS_ENABLED, true)
        }

        suspend fun getClassification(rawResult: Result, context: Context, urlClassificationDatabase: URLClassificationDatabase?): QRClassification {
            val parsedResult = ResultParser.parseResult(rawResult)
            var type =
                if (
                    parsedResult.type == ParsedResultType.URI
                    ||
                    (parsedResult.type == ParsedResultType.TEXT && (parsedResult.displayResult.startsWith("http://") || parsedResult.displayResult.startsWith("https://")))
                ) {
                    // Extract the URI
                    val uri = when (parsedResult.type) {
                        ParsedResultType.URI -> (parsedResult as URIParsedResult).uri
                        ParsedResultType.TEXT -> (parsedResult as TextParsedResult).text
                        else -> ""
                    }
                    // Check if it is a valid uri, otherwise we treat it as text
                    if (Utils.extractBaseDomainFromURI(uri) != null && URL(uri).host != null) {
                        ParsedResultType.URI
                    } else {
                        ParsedResultType.TEXT
                    }
                } else if (parsedResult.type == ParsedResultType.TEL) {
                    ParsedResultType.TEL
                } else {
                    ParsedResultType.TEXT
                }


            if (type == ParsedResultType.URI) {
                return when (parsedResult.type) {
                    ParsedResultType.URI -> getURLClassification((parsedResult as URIParsedResult).uri, context, urlClassificationDatabase)
                    ParsedResultType.TEXT -> getURLClassification((parsedResult as TextParsedResult).text, context, urlClassificationDatabase)
                    else -> {
                        // Should not happen, show as text
                        createTextClassification(rawResult.text)
                    }
                }
            } else if (type == ParsedResultType.TEL) {
                val phoneNumber = (parsedResult as TelParsedResult).number
                return QRClassification(rawResult.text, phoneNumber, GREY_PHONE, false)
            } else {
                return createTextClassification(rawResult.text)
            }
        }

        private fun createTextClassification(text: String): QRClassification {
            val shortText = if (text.length < 100) text else text.substring(0, 100) + "..."
            return QRClassification(text, shortText, GREY_TEXT, false)
        }

        private fun normalizeURL(urlString: String): String {
            val rawHost = URL(urlString).host ?: throw IllegalArgumentException("The supplied urlString $urlString does not contain a host part.")
            val intermediateURL = urlString.replace(rawHost, rawHost.lowercase())
            return if (URL(intermediateURL).protocol.lowercase() == "http") {
                "http" + intermediateURL.substring(4)
            } else if (URL(intermediateURL).protocol.lowercase() == "https") {
                "https" + intermediateURL.substring(5)
            } else {
                intermediateURL
            }
        }

        suspend fun getURLClassification(urlString: String, context: Context?, urlClassificationDatabase: URLClassificationDatabase?): QRClassification {
            return getURLClassification(urlString, context, urlClassificationDatabase, false)
        }

        private suspend fun getURLClassification(
            urlString: String,
            context: Context?,
            urlClassificationDatabase: URLClassificationDatabase?,
            networkError: Boolean
        ): QRClassification {
            var networkErrorDetected = networkError

            val rawURL = normalizeURL(urlString)
            val parsedURL = URL(rawURL)
            val host = parsedURL.host
            val protocol = parsedURL.protocol
            val path = parsedURL.path
            val parameters = parsedURL.query
            val baseDomain: String? = Utils.extractBaseDomainFromURI(rawURL)
            Log.d(
                TAG, "Performing URL classification for $rawURL ($urlString)\n" +
                        "host: $host\n" +
                        "protocol: $protocol\n" +
                        "path: $path\n" +
                        "parameters: $parameters\n" +
                        "baseDomain: $baseDomain\n"
            )

            // We set the grey case as our default
            var case = GREY_UNKNOWN

            // 1. We check if the url can be found on PhishTank
            Log.d(TAG, "Checking PhishTank for $rawURL")
            if (context != null && isPhishTankEnabled(context)) {
                when (val phishTankCheckerResult = PhishTankChecker.checkUrl(rawURL)) {
                    is PhishTankChecker.PhishResult.Success -> {
                        if (PhishTankChecker.isPhish(phishTankCheckerResult.data)) {
                            Log.d(TAG, "PhishTank detected $rawURL as phish.")
                            case = RED
                            Log.d(TAG, "Final classification for $rawURL: $case")
                            // We need to return immediately, as it could otherwise be overridden by known domains
                            return QRClassification(rawURL, baseDomain ?: rawURL, case, networkErrorDetected)
                        }
                    }

                    is PhishTankChecker.PhishResult.Error -> {
                        Log.d(TAG, "PhishTank returned an error for $rawURL")
                        networkErrorDetected = true
                    }
                }
            }

            // 2. We check if it is a redirect and resolve it. Run recursive check afterwards.
            Log.d(TAG, "Checking for redirects for $rawURL")
            if (context != null && isResolveRedirectsEnabled(context) && RedirectChecker.getRedirectURL(rawURL).isNotEmpty()) {
                val redirectedURL = RedirectChecker.getRedirectURL(rawURL)
                Log.d(TAG, "Found redirect: $rawURL -> $redirectedURL")
                if (redirectedURL != rawURL) {
                    Log.d(TAG, "Running recursive check for $redirectedURL")
                    return getURLClassification(redirectedURL, context, urlClassificationDatabase, networkErrorDetected)
                }
            }

            // 3. We check if it is a known short url service and resolve it. Run recursive check afterwards.
            Log.d(TAG, "Checking for short urls for $rawURL")
            if (context != null && isResolveRedirectsEnabled(context) && getShortLinkDomains(context).contains(baseDomain)) {
                Log.d(TAG, "Short url detected for $rawURL")
                try {
                    val redirectedURL = RedirectResolver.resolveRedirect(rawURL)
                    Log.d(TAG, "ShortLink resolved: $rawURL -> $redirectedURL")
                    if (redirectedURL.isNotEmpty() && redirectedURL != rawURL) {
                        Log.d(TAG, "Running recursive check for $redirectedURL")
                        return getURLClassification(redirectedURL, context, urlClassificationDatabase, networkErrorDetected)
                    }
                } catch (e: SocketTimeoutException) {
                    networkErrorDetected = true
                }
            }

            // 4. Check if the domain contains hex-encoded characters and decoded it. Run recursive check afterwards.
            Log.d(TAG, "Checking for hex encoding for $host")
            val hexDecodedHost = URLDecoder.decode(host, StandardCharsets.UTF_8.name())
            if (host != hexDecodedHost) {
                Log.d(TAG, "Difference between host and hex decoded version detected for $rawURL")
                Log.d(TAG, "Hex decoded: $host -> $hexDecodedHost")
                val newURL = rawURL.replace(host, hexDecodedHost)
                Log.d(TAG, "Running recursive check for $newURL")
                return getURLClassification(newURL, context, urlClassificationDatabase, networkErrorDetected)
            }

            // 5. Check if the domain contains utf8-characters that can be converted to ASCII. Run recursive check afterwards.
            Log.d(TAG, "Checking for utf8 encoding for $host")
            val asciiHost = IDN.toASCII(host)
            if (host != asciiHost) {
                Log.d(TAG, "Difference between host and ascii encoded version detected for $rawURL")
                Log.d(TAG, "ASCII encoded: $host -> $asciiHost")
                val newURL = rawURL.replace(host, asciiHost)
                Log.d(TAG, "Running recursive check for $newURL")
                return getURLClassification(newURL, context, urlClassificationDatabase, networkErrorDetected)
            }

            // 6. Check if the base domain was visited multiple times before
            Log.d(TAG, "Checking url classification database for $baseDomain")
            if (context != null && urlClassificationDatabase != null && baseDomain != null && urlClassificationDatabase.urlDao().findByURL(baseDomain) != null
                && urlClassificationDatabase.urlDao().findByURL(baseDomain)!!.visits >= getVisitsRequired(context)
            ) {
                Log.d(TAG, "Found entry with enough visits for $baseDomain")
                case = GREEN
                Log.d(TAG, "Classification for $rawURL: $case")
            }

            // 7. Check if the domain is on the known domains list
            Log.d(TAG, "Checking known domains for $baseDomain")
            if (context != null && getKnownDomains(context).contains(baseDomain)) {
                Log.d(TAG, "Found known domain for $baseDomain")
                case = GREEN
                Log.d(TAG, "Classification for $rawURL: $case")
            }
            Log.d(TAG, "Final classification for $rawURL: $case")
            return QRClassification(rawURL, baseDomain ?: rawURL, case, networkErrorDetected)
        }
    }
}
