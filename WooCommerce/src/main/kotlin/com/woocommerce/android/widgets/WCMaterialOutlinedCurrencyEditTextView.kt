package com.woocommerce.android.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.res.use
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.FILL
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.WRAP
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.pow

private const val DEFAULT_DECIMALS_NUMBER = 2

@AndroidEntryPoint
class WCMaterialOutlinedCurrencyEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleRes: Int = R.attr.wcMaterialOutlinedCurrencyEditTextViewStyle,
    private val usesFullFormatting: Boolean = false
) : TextInputLayout(ctx, attrs, defStyleRes) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-CURRENCY-VIEW-SUPER-STATE"
    }

    private lateinit var currencyEditText: CurrencyEditText

    val editText: TextInputEditText
        get() = currencyEditText

    @Inject lateinit var parameterRepository: ParameterRepository

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
    var imeOptions: Int = 0
        set(value) {
            field = value
            currencyEditText.imeOptions = value
        }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.WCMaterialOutlinedCurrencyEditTextView
        ).use { a ->
            val usesFullFormatting = a.getBoolean(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_usesFullFormatting, usesFullFormatting
            )
            currencyEditText = when (usesFullFormatting) {
                true -> FullFormattingCurrencyEditText(context)
                false -> RegularCurrencyEditText(context)
            }
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
            imeOptions = a.getInt(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_imeOptions, 0
            )
        }

        val siteParameters = parameterRepository.getParameters()

        siteParameters.currencyFormattingParameters?.let {
            when (it.currencyPosition) {
                LEFT, LEFT_SPACE -> prefixText = siteParameters.currencySymbol.orEmpty()
                RIGHT, RIGHT_SPACE -> suffixText = siteParameters.currencySymbol.orEmpty()
            }
        }
        currencyEditText.initView(siteParameters.currencyFormattingParameters)
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

private abstract class CurrencyEditText(context: Context) : TextInputEditText(context, null, R.attr.editTextStyle) {
    var supportsEmptyState: Boolean = true
    abstract var supportsNegativeValues: Boolean
    abstract val value: LiveData<BigDecimal?>

    abstract fun initView(formattingParameters: CurrencyFormattingParameters?)
    abstract fun setValue(value: BigDecimal)
}

private class RegularCurrencyEditText(context: Context) : CurrencyEditText(context), InputFilter {
    private var numberOfDecimals: Int = DEFAULT_DECIMALS_NUMBER
    private lateinit var decimalSeparator: String
    private var isChangingText = false
    private var isInitialized = false

    override var supportsNegativeValues: Boolean = false
    private val _value = MutableLiveData<BigDecimal?>()
    override val value: LiveData<BigDecimal?> = _value

    override fun initView(formattingParameters: CurrencyFormattingParameters?) {
        decimalSeparator = formattingParameters?.currencyDecimalSeparator
            ?: DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
        numberOfDecimals = formattingParameters?.currencyDecimalNumber ?: 2

        inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL or
            InputType.TYPE_NUMBER_FLAG_SIGNED
        val acceptedDigits = "0123456789.$decimalSeparator${if (supportsNegativeValues) "-" else ""}"
        keyListener = DigitsKeyListener.getInstance(acceptedDigits)
        filters = arrayOf(this)

        isInitialized = true
        if (!supportsEmptyState) {
            setValue(BigDecimal.ZERO)
            setSelection(text!!.length)
        }
    }

    override fun setValue(value: BigDecimal) {
        setText(value.toPlainString())
    }

    @Suppress("ComplexMethod")
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence {
        val newValue = StringBuilder(dest).apply {
            replace(dstart, dend, source.subSequence(start, end).toString())
        }.toString().replace(decimalSeparator, ".")

        return when {
            !supportsEmptyState && (newValue.isEmpty()) -> {
                // Prevent clearing the field if supportsEmptyState is false
                if (source.isEmpty()) "0" else ""
            }
            !supportsEmptyState && supportsNegativeValues && newValue == "0-" -> {
                // Allow entering minus sign at the end of the field if supportsEmptyState is false
                // and value is 0, we will fix the text in onTextChanged
                source
            }
            !supportsNegativeValues && newValue.contains("-") -> {
                // Prevent negative values if they are not supported
                ""
            }
            newValue.toBigDecimalOrNull() == null && newValue != "." && newValue != "-." -> {
                // Prevent entering non-valid numbers
                ""
            }
            newValue.contains(".") &&
                newValue.substringAfterLast(".").length > numberOfDecimals -> {
                // Prevent entering more decimals than what allowed
                ""
            }
            (newValue.startsWith("-0") || newValue.startsWith("00")) && source.startsWith("0") -> {
                // do not allow to have several zeros at the beginning
                ""
            }
            else -> source.toString().replace(".", decimalSeparator)
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "SwallowedException")
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        fun changeText(updatedText: String) {
            setText(updatedText)
            setSelection(updatedText.length)
        }

        if (isInitialized && !isChangingText) {
            isChangingText = true

            val text = when (text.toString()) {
                "0-" -> {
                    // The filter allows entering minus at the end of the text if supportsEmptyState is false
                    // Here we fix the ordering of the text
                    "-0".also { changeText(it) }
                }
                "-" -> {
                    // The filter allows entering minus without a number
                    // Here we fix based on supportsEmptyState value
                    if (supportsEmptyState) {
                        ""
                    } else {
                        "0"
                    }.also { changeText(it) }
                }
                else -> text
            }

            val bigDecimalValue = text?.toString()?.replace(decimalSeparator, ".")?.toBigDecimalOrNull()
            _value.value = if (supportsEmptyState) bigDecimalValue else bigDecimalValue ?: BigDecimal.ZERO

            if (text != null) {
                // Trim any leading unwanted zeros
                val cleanedText = text.trimStart('-').trimStart('0')
                if (cleanedText.isNotEmpty()) {
                    val updatedText = "${if (text.startsWith('-')) "-" else ""}$cleanedText"
                    val currentSelectionPosition = selectionStart
                    setText(updatedText)

                    try {
                        setSelection(currentSelectionPosition + updatedText.length - text.length)
                    } catch (e: IndexOutOfBoundsException) {
                        // Ignore
                    }
                }
            }

            isChangingText = false
        }
    }
}

/**
 * A [TextInputEditText] that provides full formatting experience
 */
private class FullFormattingCurrencyEditText(
    context: Context
) : CurrencyEditText(context) {
    private var isChangingText = false

    private val decimals
        get() = formattingParameters?.currencyDecimalNumber ?: 0

    private var isInitialized = false

    private var formattingParameters: CurrencyFormattingParameters? = null

    override var supportsNegativeValues: Boolean
        get() = this.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
        set(value) {
            if (value) {
                this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            } else {
                this.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

    private val _value = MutableLiveData<BigDecimal?>()
    override val value: LiveData<BigDecimal?> = _value

    override fun initView(
        formattingParameters: CurrencyFormattingParameters?
    ) {
        this.formattingParameters = formattingParameters
        isInitialized = true
        if (!supportsEmptyState) {
            setValue(BigDecimal.ZERO)
            setSelection(text!!.length)
        }
    }

    override fun setValue(value: BigDecimal) {
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
        val formattingParameters = formattingParameters
        return if (formattingParameters != null) {
            WCCurrencyUtils.formatCurrencyForDisplay(
                rawValue = value.toDouble(),
                currencyDecimalNumber = formattingParameters.currencyDecimalNumber,
                currencyDecimalSeparator = formattingParameters.currencyDecimalSeparator,
                currencyThousandSeparator = formattingParameters.currencyThousandSeparator,
                locale = Locale.ROOT
            )
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
