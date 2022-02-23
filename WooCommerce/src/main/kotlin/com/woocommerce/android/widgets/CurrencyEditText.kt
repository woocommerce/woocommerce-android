package com.woocommerce.android.widgets

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.math.min
import kotlin.math.pow

class CurrencyEditText : TextInputEditText {
    private var formatCurrency: (BigDecimal) -> String = { "" }
    private var isChangingSelection = false
    private var isChangingText = false
    private var decimals = 2

    @Suppress("SENSELESS_COMPARISON")
    private val isInitialized
        get() = formatCurrency != null

    private val _value = MutableLiveData<BigDecimal>()
    val value: LiveData<BigDecimal> = _value

    private var wcSiteSettings: WCSettingsModel? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        this.inputType = InputType.TYPE_CLASS_NUMBER
    }

    fun initView(currency: String, decimals: Int, currencyFormatter: CurrencyFormatter) {
        this.formatCurrency = currencyFormatter.buildBigDecimalFormatter(currency)
        this.decimals = decimals

        value.value?.let {
            setValue(it)
        }
    }

    fun initView(siteSettings: WCSettingsModel, decimals: Int) {
        this.wcSiteSettings = siteSettings
        this.decimals = decimals
    }

    fun setValue(value: BigDecimal) {
        isChangingText = true
        setText(formatCurrency(value))
        _value.value = value
        isChangingText = false
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (isInitialized && !isChangingText) {
            isChangingText = true

            val currentSelectionPosition = selectionStart
            clean(text, decimals)?.let { updatedValue ->
                val formattedValue = wcSiteSettings?.let {
                    WCCurrencyUtils.formatCurrencyForDisplay(updatedValue.toDouble(), it)
                } ?: text

                _value.value = updatedValue
                setText(formattedValue)
                val selectionOffset = (formattedValue?.length ?: 0) - (text?.length ?: 0)
                setSelection(currentSelectionPosition + selectionOffset)
            } ?: run {
                // TODO _value.value = null
                setText("")
            }

            isChangingText = false
        }
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
