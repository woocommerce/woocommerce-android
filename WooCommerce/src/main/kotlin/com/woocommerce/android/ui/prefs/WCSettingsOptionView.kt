package com.woocommerce.android.ui.prefs

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

/**
 * A custom clickable view that displays an option title. Used for display settings-style category
 * that routes to a view with more options when clicked.
 */
class WCSettingsOptionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(ctx, attrs, defStyleAttr, R.style.Woo_TextView_Subtitle1) {
    init {
        isFocusable = true
        isClickable = true

        // Sets the selectable background
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCSettingsOptionView)
            try {
                text = a.getString(R.styleable.WCSettingsOptionView_categoryTitle).orEmpty()
            } finally {
                a.recycle()
            }
        }
    }

    var categoryTitle: String
        get() { return text.toString() }
        set(value) { text = value }
}
