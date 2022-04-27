package com.woocommerce.android.widgets

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.textview.MaterialTextView


/**
 * Custom [MaterialTextView] with built-in selection support and automatically selects
 * all text before the action mode (copy, etc.) menu appears
 */
class WCSelectableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr), android.view.ActionMode.Callback {
    init {
        setTextIsSelectable(true)
        customSelectionActionModeCallback = this
    }

    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: Menu?): Boolean {
        selectAllText()
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

    fun selectAllText() {
        Selection.setSelection(text as Spannable, 0, length())
    }
}
