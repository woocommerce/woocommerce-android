package com.woocommerce.android.widgets

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.math.pow

class CurrencyEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {
    private var isChangingText = false
    private val decimals
        get() = siteSettings?.currencyDecimalNumber ?: 0

    private var isInitialized = false
    private var siteSettings: WCSettingsModel? = null
    var supportsNegativeValues: Boolean
        get() = this.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
        set(value) {
            if (value) {
                this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                this.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

    var supportsEmptyState: Boolean = true

    private val _value = MutableLiveData<BigDecimal>()
    val value: LiveData<BigDecimal> = _value

    fun initView(currency: String, decimals: Int, currencyFormatter: CurrencyFormatter) {
    }

    fun initView(
        siteSettings: WCSettingsModel?,
        supportsNegativeValues: Boolean,
        supportsEmptyState: Boolean
    ) {
        this.siteSettings = siteSettings
        this.supportsNegativeValues = supportsNegativeValues
        this.supportsEmptyState = supportsEmptyState
        isInitialized = true
        if (!supportsEmptyState) {
            setValue(BigDecimal.ZERO)
            setSelection(text!!.length)
        }
    }

    fun setValue(value: BigDecimal) {
        setText(value.toPlainString())
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (isInitialized && !isChangingText) {
            isChangingText = true

            val cleanValue = clean(text, decimals)
            if (cleanValue != null) {
                // When the user types backspace on a field that already contained `0`
                val shouldClearTheField = supportsEmptyState &&
                    lengthAfter < lengthBefore &&
                    _value.value?.isEqualTo(BigDecimal.ZERO) == true &&
                    cleanValue.isEqualTo(BigDecimal.ZERO)
                if (shouldClearTheField) {
                    clearValue()
                } else {
                    formatAndUpdateValue(text, cleanValue)
                }
            } else {
                clearValue()
            }
            isChangingText = false
        }
    }

    private fun formatAndUpdateValue(currentText: CharSequence?, cleanValue: BigDecimal) {
        val currentSelectionPosition = selectionStart

        val formattedValue = siteSettings?.let {
            WCCurrencyUtils.formatCurrencyForDisplay(cleanValue.toDouble(), it)
        } ?: currentText

        _value.value = cleanValue
        setText(formattedValue)
        val selectionOffset = (formattedValue?.length ?: 0) - (currentText?.length ?: 0)
        setSelection(currentSelectionPosition + selectionOffset)
    }

    private fun clearValue() {
        if (!supportsEmptyState) return
        // TODO _value.value = null
        setText("")
    }

    companion object TextCleaner {
        /**
         * Cleans the [text] so that it only has numerical characters and has the correct number of fractional digits.
         */
        fun clean(text: CharSequence?, decimals: Int): BigDecimal? {
            val nonNumericPattern = Regex("[^\\d]")
            var cleanValue = text.toString().replace(nonNumericPattern, "").toBigDecimalOrNull() ?: return null

            if (decimals > 0) {
                cleanValue = cleanValue.divide(BigDecimal(10f.pow(decimals).toInt()), decimals, HALF_UP)
            }

            return cleanValue
        }
    }
}
