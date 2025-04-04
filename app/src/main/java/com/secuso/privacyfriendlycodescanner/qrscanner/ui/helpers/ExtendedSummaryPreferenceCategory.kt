package com.secuso.privacyfriendlycodescanner.qrscanner.ui.helpers

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder

class ExtendedSummaryPreferenceCategory(context: Context, attrs: AttributeSet?) : PreferenceCategory(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(android.R.id.summary) as? TextView)?.let {
            it.isSingleLine = false
            it.maxLines = 100
        }
    }
}