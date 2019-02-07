package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import com.tooltip.Tooltip
import com.woocommerce.android.R
import java.lang.ref.WeakReference

/**
 * Used to "advertise" a new feature with a single-shot tooltip
 */
object WCFeatureTooltip {
    private const val TOOLTIP_DELAY_BEFORE_SHOWING = 1000L
    private const val TOOLTIP_DELAY_BEFORE_HIDING = 3500L
    private const val PREF_NAME = "feature_tooltip"

    /*
     * we include the version the tooltip was added both as reference to us and to provide a future means
     * to programmatically disable tooltips after a certain number of versions
     */
    enum class Feature(val prefKeyName: String, @StringRes val messageId: Int, val versionCode: Int) {
        SITE_SWITCHER("key_site_switcher", R.string.tooltip_site_switcher, 32)
    }

    fun showIfNeeded(feature: Feature, anchorView: View) {
        // do nothing if we've already shown this tooltip
        val prefs = anchorView.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(feature.prefKeyName, false)) {
           // return
        }

        // use a weak reference in case the context is no longer valid after the delay
        val weakAnchorView = WeakReference(anchorView)

        Handler().postDelayed({
            weakAnchorView.get()?.let {
                show(feature, it)
                prefs.edit().putBoolean(feature.prefKeyName, true).apply()
            }
        }, TOOLTIP_DELAY_BEFORE_SHOWING)
    }

    private fun show(feature: Feature, anchorView: View) {
        val context = anchorView.context
        val bgColor = ContextCompat.getColor(context, R.color.wc_purple)
        val textColor = ContextCompat.getColor(context, R.color.white)
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_large)

        val tooltip = Tooltip.Builder(anchorView)
                .setBackgroundColor(bgColor)
                .setTextColor(textColor)
                .setPadding(padding)
                .setGravity(Gravity.BOTTOM)
                .setText(feature.messageId)
                .show()

        // This assumes the anchor view will show a background ripple when pressed
        anchorView.isPressed = true

        val weakAnchorView = WeakReference(anchorView)
        val weakTooltip = WeakReference(tooltip)

        Handler().postDelayed({
            weakAnchorView.get()?.isPressed = false
            weakTooltip.get()?.dismiss()
        }, TOOLTIP_DELAY_BEFORE_HIDING)
    }
}
