package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.android.synthetic.main.view_material_outlined_currency_edittext.view.*
import java.math.BigDecimal

/**
 * Custom View that mimics a TextInputEditText that supports a prefix to the EditText using [CurrencyEditText].
 * This view will display a text box with a prefix. The entire view acts as a single component.
 *
 * This is a temporary view created to handle adding a prefix to the EditText. The material-components-android
 * already provides a way to add prefix to an EditText but this is currently in alpha.
 *
 * https://github.com/material-components/material-components-android/releases/tag/1.2.0-alpha01
 *
 * Once a release version is provided, we can update the [WCMaterialOutlinedEditTextView] to use a prefix
 * and deprecate this class.
 *
 */
class WCMaterialOutlinedCurrencyEditTextView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.view_material_outlined_currency_edittext, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WCMaterialOutlinedCurrencyEditTextView
            )
            try {
                // Set the edit text hint
                currency_edit_text_input.hint =
                        a.getString(R.styleable.WCMaterialOutlinedCurrencyEditTextView_currencyHint).orEmpty()
            } finally {
                a.recycle()
            }
        }
    }

    fun initialiseCurrencyEditText(
        currency: String,
        decimals: Int,
        currencyFormatter: CurrencyFormatter
    ) {
        currency_edit_text.initView(currency, decimals, currencyFormatter)
    }

    fun setText(currentValue: BigDecimal) {
        currency_edit_text.setValue(currentValue)
    }

    fun getCurrencyEditText() = currency_edit_text

    fun setError(error: String) {
        currency_edit_text_input.error = error
    }

    fun clearError() {
        currency_edit_text_input.error = null
        currency_edit_text_input.isErrorEnabled = false
    }
}
