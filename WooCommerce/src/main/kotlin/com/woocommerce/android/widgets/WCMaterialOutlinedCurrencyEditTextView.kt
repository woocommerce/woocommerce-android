package com.woocommerce.android.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.core.content.res.use
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.FILL
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.WRAP
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.text.DecimalFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.pow

@AndroidEntryPoint
class WCMaterialOutlinedCurrencyEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleRes: Int = R.attr.wcMaterialOutlinedCurrencyEditTextViewStyle
) : TextInputLayout(ctx, attrs, defStyleRes) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-CURRENCY-VIEW-SUPER-STATE"
    }

    private val currencyEditText: CurrencyEditText = CurrencyEditText(context).apply {
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
    }

    val editText: TextInputEditText
        get() = currencyEditText

    @Inject lateinit var wcStore: WooCommerceStore
    @Inject lateinit var selectedSite: SelectedSite

    var supportsNegativeValues: Boolean = true
        set(value) {
            field = value
            currencyEditText.supportsNegativeValues = value
        }
    var supportsEmptyState: Boolean = true
        set(value) {
            field = value
            currencyEditText.supportsEmptyState = value
        }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.WCMaterialOutlinedCurrencyEditTextView
        ).use { a ->
            val mode = EditTextLayoutMode.values()[
                a.getInt(R.styleable.WCMaterialOutlinedCurrencyEditTextView_editTextLayoutMode, FILL.ordinal)
            ]
            val width = when (mode) {
                FILL -> ViewGroup.LayoutParams.MATCH_PARENT
                WRAP -> ViewGroup.LayoutParams.WRAP_CONTENT
            }
            currencyEditText.layoutParams = LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            addView(currencyEditText)
            isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_enabled, true)
            if (a.hasValue(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_textSize)) {
                currencyEditText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_textSize, 0f)
                )
                prefixTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_textSize, 0f)
                )
                suffixTextView.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_textSize, 0f)
                )
            }
            currencyEditText.gravity = a.getInt(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_gravity, currencyEditText.gravity
            )
            supportsNegativeValues = a.getBoolean(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_supportsNegativeValues, supportsNegativeValues
            )
            supportsEmptyState = a.getBoolean(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_supportsEmptyState, supportsEmptyState
            )
        }

        val siteSettings = selectedSite.getIfExists()?.let {
            wcStore.getSiteSettings(it)
        }

        siteSettings?.let {
            val currencySymbol = wcStore.getSiteCurrency(selectedSite.get(), siteSettings.currencyCode)
            when (siteSettings.currencyPosition) {
                LEFT, LEFT_SPACE -> prefixText = currencySymbol
                RIGHT, RIGHT_SPACE -> suffixText = currencySymbol
            }
        }
        currencyEditText.initView(
            siteSettings = siteSettings,
            supportsNegativeValues = supportsNegativeValues,
            supportsEmptyState = supportsEmptyState
        )
    }

    val value: LiveData<BigDecimal?>
        get() = currencyEditText.value

    fun getText() = currencyEditText.text.toString()

    fun setValue(currentValue: BigDecimal) {
        currencyEditText.setValue(currentValue)
    }

    /**
     * Updates the value only if the current one is different from the supplied one.
     * Helpful when binding the value to a state in the ViewModel without losing the cursor position
     */
    fun setValueIfDifferent(newValue: BigDecimal) {
        if (newValue isNotEqualTo currencyEditText.value.value) {
            setValue(newValue)
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        currencyEditText.onSaveInstanceState()?.let {
            bundle.putParcelable(KEY_SUPER_STATE, WCSavedState(super.onSaveInstanceState(), it))
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = (state as? Bundle)?.getParcelable<WCSavedState>(KEY_SUPER_STATE)?.let {
            restoreViewState(it)
        } ?: state
        super.onRestoreInstanceState(bundle)
    }

    private fun restoreViewState(state: WCSavedState): Parcelable {
        currencyEditText.onRestoreInstanceState(state.savedState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchThawSelfOnly(container)
    }

    private enum class EditTextLayoutMode {
        FILL, WRAP
    }
}

private class CurrencyEditText @JvmOverloads constructor(
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
        get() = this.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
        set(value) {
            if (value) {
                this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            } else {
                this.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

    var supportsEmptyState: Boolean = true

    private val _value = MutableLiveData<BigDecimal?>()
    val value: LiveData<BigDecimal?> = _value

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
        setText(formatValue(value))
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

        val (formattedValue, selectionPosition) =
            if (currentText?.startsWith("-") == true && cleanValue.isEqualTo(BigDecimal.ZERO)) {
                // A special case for negative values if the actual value is still 0
                val value = "-${formatValue(cleanValue)}"
                Pair(value, value.length)
            } else {
                val value = formatValue(cleanValue)
                val selectionOffset = value.length - (currentText?.length ?: 0)
                Pair(value, currentSelectionPosition + selectionOffset)
            }

        _value.value = cleanValue
        setText(formattedValue)
        setSelection(max(0, selectionPosition))
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
    }

    private fun clearValue() {
        if (supportsEmptyState) {
            _value.value = null
            setText("")
        } else {
            setValue(BigDecimal.ZERO)
        }
    }

    private fun formatValue(value: BigDecimal): String {
        val siteSettings = siteSettings
        return if (siteSettings != null) {
            WCCurrencyUtils.formatCurrencyForDisplay(value.toDouble(), siteSettings, Locale.ROOT)
        } else {
            val decimalFormat = DecimalFormat("0.${"0".repeat(decimals)}")
            decimalFormat.format(value)
        }
    }

    companion object TextCleaner {
        /**
         * Cleans the [text] so that it only has numerical characters and has the correct number of fractional digits.
         */
        fun clean(text: CharSequence?, decimals: Int): BigDecimal? {
            val nonNumericPattern = Regex("[^0-9\\-]")
            var cleanValue = text.toString().replace(nonNumericPattern, "").toBigDecimalOrNull() ?: return null

            if (decimals > 0) {
                cleanValue = cleanValue.divide(BigDecimal(10f.pow(decimals).toInt()), decimals, HALF_UP)
            }

            return cleanValue
        }
    }
}
