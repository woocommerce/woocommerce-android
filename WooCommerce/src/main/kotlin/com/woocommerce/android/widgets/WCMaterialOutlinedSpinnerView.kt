package com.woocommerce.android.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.databinding.ViewMaterialOutlinedSpinnerBinding
import com.woocommerce.android.extensions.setHtmlText

/**
 * Custom View that mimics a TextInputEditText with a spinner that opens a selector dialog it.
 * This view will display a text box which will open a dialog when clicked.
 * The entire view acts as a single component.
 */
class WCMaterialOutlinedSpinnerView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.wcMaterialOutlinedSpinnerViewStyle
) : TextInputLayout(ctx, attrs, defStyleAttr) {
    private val binding = ViewMaterialOutlinedSpinnerBinding.inflate(LayoutInflater.from(context), this)

    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-SPINNER-VIEW-SUPER-STATE"
    }

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCMaterialOutlinedSpinnerView)
            try {
                // Set the startup text
                a.getString(R.styleable.WCMaterialOutlinedSpinnerView_android_text)?.let {
                    setText(it)
                }

                isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedSpinnerView_android_enabled, true)
            } finally {
                a.recycle()
            }
        }
    }

    fun setClickListener(onClickListener: ((view: View) -> Unit)) {
        binding.spinnerEditText.setOnClickListener(onClickListener)
    }

    fun setText(selectedText: String) {
        binding.spinnerEditText.setText(selectedText)
    }

    fun setText(@StringRes selectedTextRes: Int) {
        binding.spinnerEditText.setText(selectedTextRes)
    }

    fun setHtmlText(selectedText: String) {
        binding.spinnerEditText.setHtmlText(selectedText)
    }

    fun getText() = binding.spinnerEditText.text.toString()

    fun <T> setup(
        values: Array<T>,
        onSelected: (T) -> Unit,
        mapper: (T) -> String = { it.toString() }
    ) {
        val textValues = values.map(mapper).toTypedArray()
        setClickListener {
            MaterialAlertDialogBuilder(ContextThemeWrapper(context, style.Theme_Woo_DayNight))
                .setTitle(hint)
                .setSingleChoiceItems(textValues, textValues.indexOf(getText())) { dialog, which ->
                    dialog.dismiss()
                    onSelected(values[which])
                }
                .show()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        binding.spinnerEditText.onSaveInstanceState()?.let {
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
        binding.spinnerEditText.onRestoreInstanceState(state.savedState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchThawSelfOnly(container)
    }
}
