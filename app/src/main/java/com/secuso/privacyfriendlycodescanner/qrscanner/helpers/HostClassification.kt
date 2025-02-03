package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import android.content.Context
import android.util.TypedValue
import com.secuso.privacyfriendlycodescanner.qrscanner.R

class HostClassification(val uri: String, val case: Case) {
    enum class Case {
        GREEN, BLUE, GRAY;

        fun getColor(context: Context): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            return when (this) {
                GREEN -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGreen, typedValue, true)
                    typedValue.data
                }

                BLUE -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationBlue, typedValue, true)
                    typedValue.data
                }

                GRAY -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGray, typedValue, true)
                    typedValue.data
                }
            }
        }

        fun getTexts(): Array<Int> = when (this) {
            GREEN -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_part_1, R.string.url_dialog_risk_explanation_low_risk_part_2)
            BLUE -> arrayOf(R.string.url_dialog_risk_explanation_blue_risk_part_1, R.string.url_dialog_risk_explanation_blue_risk_part_2)
            GRAY -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_part_1, R.string.url_dialog_risk_explanation_unknown_risk_part_2)
        }
    }

    companion object {
        val HOSTS_LIST = setOf("example.com", "kit.edu", "knownpage.com")
        val BLUE_CASE_VISITS_REQUIRED = 1
        val GRAY_CASE_WAITING_TIME = 3_000 //Time in millis
    }
}