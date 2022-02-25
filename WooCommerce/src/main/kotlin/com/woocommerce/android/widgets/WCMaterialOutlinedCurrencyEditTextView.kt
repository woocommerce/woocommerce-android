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
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.core.content.res.use
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.FILL
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView.EditTextLayoutMode.WRAP
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

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

    fun setNonFloatingHint(text: CharSequence?) {
        if (!text.isNullOrEmpty()) {
            // Disable the floating hint first
            isHintEnabled = false
        }
        editText.hint = text
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
    private var isInitialized = false
    private var siteSettings: WCSettingsModel? = null
    private val numberOfDecimals: Int
        get() = siteSettings?.currencyDecimalNumber ?: 2

    var supportsNegativeValues: Boolean
        get() = this.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
        set(value) {
            if (value) {
                this.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            } else {
                this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
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

        val decimalSeparator =
            siteSettings?.currencyDecimalSeparator ?: DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()

        val acceptedDigits = "0123456789.$decimalSeparator${if(supportsNegativeValues) "-" else ""}"
        keyListener = DigitsKeyListener.getInstance(acceptedDigits)
        filters = arrayOf(InputFilter { source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int ->
            val newValue = StringBuilder(dest).apply {
                replace(dstart, dend, source.subSequence(start, end).toString())
            }.toString().replace(decimalSeparator, ".")
            return@InputFilter when {
                !supportsEmptyState && newValue.isEmpty() -> {
                    if (source.isEmpty()) "0" else ""
                }
                newValue.toBigDecimalOrNull() == null -> ""
                newValue.contains(".") &&
                    newValue.substringAfterLast(".").length > numberOfDecimals -> ""
                else -> source.toString().replace(".", decimalSeparator)
            }
        })

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

            _value.value = text?.toString()?.toBigDecimalOrNull()
            if (text != null) {
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
