package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
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
    GestureDetector.OnDoubleTapListener {
    private val detector = GestureDetector(context, GestureDetector.SimpleOnGestureListener())

    // when text is selectable, TextView intercepts the click event even if there's no OnClickListener,
    // requiring this workaround to pass the click to a parent view
    private var clickableParent: View? = null

    init {
        setTextIsSelectable(true)
        customSelectionActionModeCallback = this

        detector.setOnDoubleTapListener(this)

        // noinspection ClickableViewAccessibility
        setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }
    }

    fun setClickableParent(view: View?) {
        clickableParent = view
    }

    // -- ActionMode.Callback -- used to detect when the Copy menu appears

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

    override fun onDestroyActionMode(mode: android.view.ActionMode?) {
        // noop
    }

    // -- OnDoubleTapListener -- used to detect the click so we can pass it along -- note that we
    // can't simply set the OnClickListener because of an Android bug that causes TextView to
    // require two clicks when text is selectable

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return clickableParent?.performClick() ?: false
    }

    override fun onDoubleTap(e: MotionEvent) = false

    override fun onDoubleTapEvent(e: MotionEvent) = false
}
