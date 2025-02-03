package com.secuso.privacyfriendlycodescanner.qrscanner.ui.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import com.secuso.privacyfriendlycodescanner.qrscanner.R

/**
 * Wrapper class to easily set dialog attributes in tutorial slides.
 */
class ScannerUrlDialogWrapper(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    @StringRes
    private val textPart1: Int

    @StringRes
    private val textPart2: Int

    private val textUrl: String?
    private val dialogColor: ColorStateList?

    init {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.ScannerUrlDialogWrapper, 0, 0)

        textPart1 = array.getResourceId(R.styleable.ScannerUrlDialogWrapper_textPart1, 0)
        textPart2 = array.getResourceId(R.styleable.ScannerUrlDialogWrapper_textPart2, 0)
        textUrl = array.getString(R.styleable.ScannerUrlDialogWrapper_textUrl)

        val color = array.getColor(R.styleable.ScannerUrlDialogWrapper_dialogColor, 0)
        dialogColor = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(color))

        array.recycle()
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        (child.findViewById<View>(R.id.url_dialog_risk_explanation_part_1) as TextView).setText(textPart1)
        (child.findViewById<View>(R.id.url_dialog_risk_explanation_part_2) as TextView).setText(textPart2)

        findViewById<Button>(R.id.url_dialog_domain).text = textUrl

        (findViewById<View>(R.id.dialog_border) as ImageView).imageTintList = dialogColor
        findViewById<View>(R.id.url_dialog_continue_button).backgroundTintList = dialogColor
    }
}
