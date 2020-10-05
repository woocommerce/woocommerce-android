package com.woocommerce.android.ui.prefs

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StyleRes
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.StyleAttrUtils
import kotlinx.android.synthetic.main.view_option_with_active_setting.view.*

/**
 * A custom clickable view that displays an option title and the active selected value. Used for
 * settings-style options that when clicked the option can be changed by for example a dialog. The new
 * selection will then be displayed in the [optionValue] view.
 */
class WCSettingsOptionValueView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsOptionValueStyle,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.view_option_with_active_setting, this)
        orientation = VERTICAL
        isFocusable = true
        isClickable = true

        // Sets the selectable background
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        if (attrs != null) {
            val a = context
                    .obtainStyledAttributes(attrs, R.styleable.WCSettingsOptionValueView, defStyleAttr, defStyleRes)
            try {
                // Set the view title and style
                option_title.text = StyleAttrUtils.getString(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsOptionValueView_optionTitle,
                        R.styleable.WCSettingsOptionValueView_tools_optionTitle)

                // Set the active option
                StyleAttrUtils.getString(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsOptionValueView_optionValue,
                        R.styleable.WCSettingsOptionValueView_tools_optionValue
                )?.let {
                    option_value.visibility = View.VISIBLE
                    option_value.text = it
                }

                // Set max lines
                if (a.hasValue(R.styleable.WCSettingsOptionValueView_optionValueMaxLines)) {
                    option_value.maxLines = StyleAttrUtils.getInt(
                            a,
                            isInEditMode,
                            R.styleable.WCSettingsOptionValueView_optionValueMaxLines,
                            R.styleable.WCSettingsOptionValueView_tools_optionValueMaxLines,
                            2
                    )
                }
            } finally {
                a.recycle()
            }
        }
    }

    var optionTitle: String
        get() { return option_title.text.toString() }
        set(value) { option_title.text = value }

    var optionValue: String?
        get() { return option_value.text.toString() }
        set(value) {
            if (value.isNullOrEmpty()) {
                option_value.text = StringUtils.EMPTY
                option_value.visibility = View.GONE
            } else {
                option_value.text = value
                option_value.visibility = View.VISIBLE
            }
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        option_title?.isEnabled = enabled
        option_value?.isEnabled = enabled
    }
}
