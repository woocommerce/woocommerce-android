package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_material_outlined_edittext.view.*

/**
 * Custom View that mimics a TextInputEditText with a spinner that opens a selector dialog it.
 * This view will display a text box which will open a dialog when clicked.
 * The entire view acts as a single component.
 */
class WCMaterialOutlinedSpinnerView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.view_material_outlined_spinner, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCMaterialOutlinedSpinnerView)
            try {
                // Set the edit text spinner hint
                edit_text_input.hint = a.getString(R.styleable.WCMaterialOutlinedSpinnerView_spinnerHint).orEmpty()
            } finally {
                a.recycle()
            }
        }
    }

    fun setClickListener(onClickListener: ((view: View) -> Unit)) {
        edit_text.setOnClickListener(onClickListener)
    }

    fun setText(selectedText: String) {
        edit_text.setText(selectedText)
    }

    fun getText() = edit_text.text.toString()
}
