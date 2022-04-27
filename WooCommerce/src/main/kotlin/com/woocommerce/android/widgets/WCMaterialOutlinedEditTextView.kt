package com.woocommerce.android.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewMaterialOutlinedEdittextBinding
import org.wordpress.android.util.ActivityUtils

/**
 * Custom View that encapsulates a [TextInputLayout] and [TextInputEditText], and as such has the following
 * capabilities:
 * - Show helper text
 * - Show error text
 * - Enable/set counter and maxLength
 * - Set the text on the child [TextInputEditText]
 * - Set the selected text on the child [TextInputEditText]
 *
 * The entire view acts as a single component. The following attributes have been exposed to this view as
 * custom attributes and are used to interact with the nested [TextInputEditText] component:
 * - [android:inputType]
 * - [android:maxLength]
 * - [android:enabled]
 * - [android:text]
 */
class WCMaterialOutlinedEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.wcMaterialOutlinedEditTextViewStyle
) : TextInputLayout(ctx, attrs, defStyleAttr) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-EDITTEXT-VIEW-SUPER-STATE"
    }

    private val binding = ViewMaterialOutlinedEdittextBinding.inflate(LayoutInflater.from(context), this)

    var textWatcher: TextWatcher? = null
        set(value) {
            field = value
            value?.let { binding.editText.addTextChangedListener(it) }
        }

    var text: String
        get() = binding.editText.text.toString()
        set(value) = binding.editText.setText(value)

    var imeOptions: Int
        get() = binding.editText.imeOptions
        set(value) {
            binding.editText.imeOptions = value
        }

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.WCMaterialOutlinedEditTextView
            )
            try {
                // Set the edit text input type
                binding.editText.inputType = a.getInt(
                    R.styleable.WCMaterialOutlinedEditTextView_android_inputType,
                    EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                )

                // Set the max length
                if (a.hasValue(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength)) {
                    val max = a.getInt(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength, 0)
                    setMaxLength(max)
                }

                // Set the startup text
                a.getString(R.styleable.WCMaterialOutlinedSpinnerView_android_text)?.let {
                    text = it
                }

                // only set imeOptions if defined, otherwise we'll override the default value
                if (a.hasValue(R.styleable.WCMaterialOutlinedEditTextView_android_imeOptions)) {
                    imeOptions = a.getInt(
                        R.styleable.WCMaterialOutlinedEditTextView_android_imeOptions, 0
                    )
                }

                isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_enabled, true)
            } finally {
                a.recycle()
            }
        }
    }

    /**
     * Updates the text only if the current content is different from the supplied one.
     * Helpful when this view is inside a RecyclerView to avoid resetting the cursor position and recursive listener
     * events.
     */
    fun setTextIfDifferent(newText: String) {
        if (text != newText) {
            text = newText
            binding.editText.setSelection(newText.length)
        }
    }

    fun setSelection(start: Int, stop: Int) {
        binding.editText.setSelection(start, stop)
    }

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        binding.editText.doAfterTextChanged {
            cb(it)
        }
    }

    fun setOnEditorActionListener(cb: (text: String) -> Boolean) {
        binding.editText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                val text = binding.editText.text.toString()
                if (text.isNotEmpty()) {
                    cb.invoke(text)
                } else false
            } else {
                false
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun setMaxLength(max: Int) {
        binding.editText.filters += InputFilter.LengthFilter(max)
    }

    fun showKeyboard(selectAll: Boolean = false) {
        if (binding.editText.requestFocus()) {
            if (selectAll) {
                binding.editText.selectAll()
            }
            binding.editText.postDelayed(
                {
                    ActivityUtils.showKeyboard(binding.editText)
                },
                100
            )
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        binding.editText.onSaveInstanceState()?.let {
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
        binding.editText.onRestoreInstanceState(state.savedState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchThawSelfOnly(container)
    }
}
