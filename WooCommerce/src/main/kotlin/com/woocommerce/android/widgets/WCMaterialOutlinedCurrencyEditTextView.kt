package com.woocommerce.android.widgets

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
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
class WCMaterialOutlinedCurrencyEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleRes: Int = R.attr.wcMaterialOutlinedCurrencyEditTextViewStyle
) : TextInputLayout(ctx, attrs, defStyleRes) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-CURRENCY-VIEW-SUPER-STATE"
    }
    init {
        View.inflate(context, R.layout.view_material_outlined_currency_edittext, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WCMaterialOutlinedCurrencyEditTextView
            )
            try {
                isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_enabled, true)
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

    fun getText() = currency_edit_text.text.toString()

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        currency_edit_text.isEnabled = enabled
    }

    fun getCurrencyEditText(): CurrencyEditText = currency_edit_text

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        currency_edit_text.onSaveInstanceState()?.let {
            bundle.putParcelable(KEY_SUPER_STATE, SavedState(super.onSaveInstanceState(), it))
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = (state as? Bundle)?.getParcelable<SavedState>(KEY_SUPER_STATE)?.let {
            restoreViewState(it)
        } ?: state
        super.onRestoreInstanceState(bundle)
    }

    private fun restoreViewState(state: SavedState): Parcelable {
        currency_edit_text.onRestoreInstanceState(state.editTextState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    internal class SavedState : BaseSavedState {
        internal var editTextState: Parcelable? = null

        constructor(superState: Parcelable?, inEditTextState: Parcelable) : super(superState) {
            editTextState = inEditTextState
        }

        /**
         * Workaround to differentiate between this method and the one that requires API 24+ because
         * the super(source, loader) method won't work on older APIs - thus the app will crash.
         */
        constructor(source: Parcel, loader: ClassLoader?, superState: Parcelable?): super(superState) {
            editTextState = source.readParcelable<Parcelable>(loader)
        }

        constructor(source: Parcel) : super(source) {
            editTextState = source.readParcelable(this::class.java.classLoader)
        }

        @RequiresApi(VERSION_CODES.N)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            editTextState = loader?.let {
                source.readParcelable<Parcelable>(it)
            } ?: source.readParcelable<Parcelable>(this::class.java.classLoader)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(editTextState, 0)
        }

        companion object {
            @Suppress("UNUSED")
            @JvmField
            val CREATOR = object : Parcelable.ClassLoaderCreator<SavedState> {
                override fun createFromParcel(source: Parcel, loader: ClassLoader?): SavedState {
                    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
                        SavedState(source, loader)
                    } else {
                        SavedState(source, loader, source.readParcelable<Parcelable>(loader))
                    }
                }

                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
