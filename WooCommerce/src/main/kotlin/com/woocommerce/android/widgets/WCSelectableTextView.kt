package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.databinding.WcSelectableTextviewBinding

/**
 * Custom [MaterialTextView] with built-in selection support and a copy button at the end
 */
class WCSelectableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), android.view.ActionMode.Callback {
    private val binding = WcSelectableTextviewBinding.inflate(LayoutInflater.from(context))

    init {
        setTextIsSelectable(true)
        customSelectionActionModeCallback = this
    }

    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: Menu?): Boolean {
        isSelected = true
        return true
    }

    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: android.view.ActionMode?, item: MenuItem?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: android.view.ActionMode?) {
        // noop
    }
}
