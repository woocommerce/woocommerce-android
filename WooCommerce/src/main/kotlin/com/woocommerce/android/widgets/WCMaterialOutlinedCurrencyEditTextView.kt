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
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.use
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.FILL
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.WRAP
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT_SPACE
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

private const val DEFAULT_DECIMALS_NUMBER = 2

@AndroidEntryPoint
class WCMaterialOutlinedCurrencyEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleRes: Int = R.attr.wcMaterialOutlinedCurrencyEditTextViewStyle
) : TextInputLayout(ctx, attrs, defStyleRes) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-CURRENCY-VIEW-SUPER-STATE"
    }

    private val currencyEditText = CurrencyEditText(context)

    @Inject
    lateinit var parameterRepository: ParameterRepository

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

    val siteParameters = parameterRepository.getParameters()

    var orderCurrency: String? = null
        set(value) {
            field = value
            siteParameters.currencyFormattingParameters?.let {
                when (it.currencyPosition) {
                    LEFT, LEFT_SPACE -> prefixText = orderCurrency
                    RIGHT, RIGHT_SPACE -> suffixText = orderCurrency
                }
            }
        }

    val value: LiveData<BigDecimal?>
        get() = currencyEditText.value

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
            imeOptions = a.getInt(
                R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_imeOptions, 0
            )
        }

        siteParameters.currencyFormattingParameters?.let {
            when (it.currencyPosition) {
                LEFT, LEFT_SPACE -> prefixText = orderCurrency ?: siteParameters.currencySymbol.orEmpty()
                RIGHT, RIGHT_SPACE -> suffixText = orderCurrency ?: siteParameters.currencySymbol.orEmpty()
            }
        }
        currencyEditText.initView(siteParameters.currencyFormattingParameters)
    }

    override fun getEditText(): TextInputEditText {
        return currencyEditText
    }

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
        val bundle = (state as? Bundle)?.parcelable<WCSavedState>(KEY_SUPER_STATE)?.let {
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

private class CurrencyEditText(context: Context) :
    TextInputEditText(context, null, androidx.appcompat.R.attr.editTextStyle),
    InputFilter {
    private var isChangingText = false
    private var isInitialized = false

    private lateinit var decimalSeparator: String

    var supportsNegativeValues: Boolean = false
        set(value) {
            field = value
            if (this::inputHandler.isInitialized) {
                inputHandler.supportsNegativeValues = value
            }
        }

    var supportsEmptyState: Boolean = true
        set(value) {
            field = value
            if (this::inputHandler.isInitialized) {
                inputHandler.supportsEmptyState = value
            }
        }

    private val _value = MutableLiveData<BigDecimal?>()
    val value: LiveData<BigDecimal?> = _value

    private lateinit var inputHandler: CurrencyInputHandler

    fun initView(formattingParameters: CurrencyFormattingParameters?) {
        decimalSeparator = formattingParameters?.currencyDecimalSeparator
            ?: DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()

        inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL or
            InputType.TYPE_NUMBER_FLAG_SIGNED
        val acceptedDigits = "0123456789.$decimalSeparator${if (supportsNegativeValues) "-" else ""}"
        keyListener = DigitsKeyListener.getInstance(acceptedDigits)
        filters = arrayOf(this)

        inputHandler = CurrencyInputHandler(
            supportsEmptyState = supportsEmptyState,
            supportsNegativeValues = supportsNegativeValues,
            decimalSeparator = decimalSeparator,
            numberOfDecimals = formattingParameters?.currencyDecimalNumber ?: DEFAULT_DECIMALS_NUMBER
        )

        isInitialized = true
        if (!supportsEmptyState) {
            setValue(BigDecimal.ZERO)
            setSelection(text!!.length)
        }
    }

    fun setValue(value: BigDecimal) {
        setText(value.toPlainString())
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence {
        if (isChangingText) return source.toString().replace(".", decimalSeparator)
        return inputHandler.filter(source, start, end, dest, dstart, dend)
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "SwallowedException")
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (isInitialized && !isChangingText) {
            isChangingText = true

            val adjustedText = inputHandler.adjustText(text)

            _value.value = adjustedText.toString().replace(decimalSeparator, ".").toBigDecimalOrNull()
            val currentSelectionPosition = selectionStart

            setText(adjustedText)
            val selection = (currentSelectionPosition + adjustedText.length - (text?.length ?: 0))
                .coerceIn(0, adjustedText.length)
            setSelection(selection)

            isChangingText = false
        }
    }
}

@VisibleForTesting
class CurrencyInputHandler(
    var supportsEmptyState: Boolean,
    var supportsNegativeValues: Boolean,
    val decimalSeparator: String,
    val numberOfDecimals: Int
) {
    @Suppress("LongParameterList", "ComplexMethod")
    fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: CharSequence,
        dstart: Int,
        dend: Int
    ): CharSequence {
        val newValue = StringBuilder(dest).apply {
            replace(dstart, dend, source.subSequence(start, end).toString())
        }.toString().replace(decimalSeparator, ".")

        return when {
            !supportsEmptyState && newValue.isEmpty() -> {
                // Prevent clearing the field if supportsEmptyState is false
                "0"
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
            supportsNegativeValues && newValue == "-" -> {
                // Allow entering minus sign
                source
            }
            newValue.toBigDecimalOrNull() == null -> {
                // Prevent entering non-valid numbers
                ""
            }
            newValue.contains(".") &&
                newValue.substringAfterLast(".").length > numberOfDecimals -> {
                // Prevent entering more decimals than what allowed
                ""
            }
            else -> source.toString().replace(".", decimalSeparator)
        }
    }

    fun adjustText(text: CharSequence?): CharSequence {
        if (text == null) return ""

        val updatedText = when {
            text.toString() == "0-" -> "-0"
            text.toString() == "-" && !supportsEmptyState -> "0"
            text.matches("^-?0+\\d+".toRegex()) -> text.replace("^(-?)0+(\\d+)".toRegex(), "$1$2")
            else -> text
        }

        return updatedText
    }
}
