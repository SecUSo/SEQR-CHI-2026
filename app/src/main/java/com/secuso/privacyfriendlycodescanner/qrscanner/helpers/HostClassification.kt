package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.secuso.privacyfriendlycodescanner.qrscanner.R

class HostClassification(val uri: String, val case: Case) {
    enum class Case {
        GREEN, BLUE, GRAY;

        fun getColorStateList(context: Context): ColorStateList {
            val typedValue = TypedValue()
            val theme = context.theme
            return when (this) {
                GREEN -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGreen, typedValue, true)
                    ColorStateList(arrayOf<IntArray>(intArrayOf(android.R.attr.state_enabled)), intArrayOf(typedValue.data))
                }

                BLUE -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationBlue, typedValue, true)
                    ColorStateList(arrayOf<IntArray>(intArrayOf(android.R.attr.state_enabled)), intArrayOf(typedValue.data))
                }

                GRAY -> {
                    theme.resolveAttribute(R.attr.colorHostClassificationGray, typedValue, true)
                    ContextCompat.getColorStateList(context, typedValue.resourceId)!!
                }
            }
        }

        fun getButtonTextColorStateList(context: Context): ColorStateList {
            return when (this) {
                GREEN -> ContextCompat.getColorStateList(context, R.color.url_classification_green_case_button_text)!!
                BLUE -> ContextCompat.getColorStateList(context, R.color.url_classification_blue_case_button_text)!!
                GRAY -> ContextCompat.getColorStateList(context, R.color.url_classification_grey_case_button_text)!!
            }
        }

        fun getTexts(): Array<Int> = when (this) {
            GREEN -> arrayOf(R.string.url_dialog_risk_explanation_low_risk_part_1, R.string.url_dialog_risk_explanation_low_risk_part_2)
            BLUE -> arrayOf(R.string.url_dialog_risk_explanation_blue_risk_part_1, R.string.url_dialog_risk_explanation_blue_risk_part_2)
            GRAY -> arrayOf(R.string.url_dialog_risk_explanation_unknown_risk_part_1, R.string.url_dialog_risk_explanation_unknown_risk_part_2)
        }
    }
}