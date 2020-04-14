package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_material_outlined_spinner.view.*

/**
 * Custom View that mimics a TextInputEditText with a spinner that opens a selector dialog it.
 * This view will display a text box which will open a dialog when clicked.
 * The entire view acts as a single component.
 */
class WCMaterialOutlinedSpinnerView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.view_material_outlined_spinner, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCMaterialOutlinedSpinnerView)
            try {
                // Set the edit text spinner hint
                spinner_edit_text_input.hint =
                        a.getString(R.styleable.WCMaterialOutlinedSpinnerView_spinnerHint).orEmpty()
            } finally {
                a.recycle()
            }
        }
    }

    fun setClickListener(onClickListener: ((view: View) -> Unit)) {
        spinner_edit_text.setOnClickListener(onClickListener)
    }

    fun setText(selectedText: String) {
        spinner_edit_text.post { spinner_edit_text.setText(selectedText) }
    }

    fun getText() = spinner_edit_text.text.toString()
}
