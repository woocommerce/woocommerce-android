package com.woocommerce.android.widgets

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.view_material_outlined_edittext.view.*

/**
 * Custom View that mimics a TextInputEditText with a summary TextView below it. This view will display
 * a text box and a summary. The entire view acts as a single component.
 *
 */
class WCMaterialOutlinedEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.view_material_outlined_edittext, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WCMaterialOutlinedEditTextView
            )
            try {
                // Set the edit text hint
                edit_text_input.hint = a.getString(R.styleable.WCMaterialOutlinedEditTextView_editTextHint)
                        .orEmpty()

                // Set the edit text summary
                a.getString(R.styleable.WCMaterialOutlinedEditTextView_editTextSummary)?.let {
                    edit_text_input.isHelperTextEnabled = true
                    edit_text_input.helperText = it
                }

                // Set the edit text input type
                edit_text.inputType = a.getInt(
                        R.styleable.WCMaterialOutlinedEditTextView_android_inputType,
                        EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                )

                // Set the max length
                if (a.hasValue(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength)) {
                    val max = a.getInt(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength, 0)
                    setMaxLength(max)
                }
            } finally {
                a.recycle()
            }
        }
    }

    fun setText(selectedText: String) {
        edit_text.setText(selectedText)
    }

    fun setHint(hintStr: String) {
        edit_text_input.hint = hintStr
    }

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        edit_text.doAfterTextChanged {
            clearError()
            cb(it)
        }
    }

    fun setError(error: String) {
        edit_text_input.error = error
    }

    fun clearError() {
        edit_text_input.isErrorEnabled = false
        edit_text_input.error = StringUtils.EMPTY
    }

    fun setMaxLength(max: Int) {
        edit_text.filters += InputFilter.LengthFilter(max)
    }
}
