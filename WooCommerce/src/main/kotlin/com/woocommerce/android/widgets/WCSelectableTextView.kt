package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.selectAllText
import org.wordpress.android.util.ToastUtils

/**
 * Custom [MaterialTextView] with built-in text selection support and automatically selects
 * all text before the action mode menu (Copy, etc.) appears
 */
class WCSelectableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr),
    android.view.ActionMode.Callback,
    GestureDetector.OnDoubleTapListener{
    private val detector = GestureDetectorCompat(context, GestureDetector.SimpleOnGestureListener())

    init {
        setTextIsSelectable(true)
        customSelectionActionModeCallback = this
        detector.setOnDoubleTapListener(this)

        // noinspection ClickableViewAccessibility
        setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    // -- ActionMode.Callback

    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: Menu?): Boolean {
        selectAllText()
        return true
    }

    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: Menu?) = false

    override fun onActionItemClicked(mode: android.view.ActionMode?, item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.copy) {
            ToastUtils.showToast(context, R.string.copied_to_clipboard)
        }
        return false
    }

    override fun onDestroyActionMode(mode: android.view.ActionMode?) { }

    // -- OnDoubleTapListener

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        ToastUtils.showToast(context, "Tap!")
        return false
    }

    override fun onDoubleTap(e: MotionEvent?) = false

    override fun onDoubleTapEvent(e: MotionEvent?) = false
}
