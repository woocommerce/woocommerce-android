package com.woocommerce.android.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

typealias OnLinkClicked = (ClickableSpan) -> Unit

/**
 * Sets the TextView text from a raw HTML string.
 *
 * Optionally, provide an extra [onLinkClicked] callback to be triggered when any of the links in the HTML are clicked.
 * The callback is triggered in addition to the default link handling behavior, it does not override it.
 */
fun TextView.setHtmlText(html: String, onLinkClicked: OnLinkClicked? = null) {
    val spannedHtml = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(html)
    }

    val strBuilder = SpannableStringBuilder(spannedHtml)
    onLinkClicked?.let { callback ->
        // If we were given a custom listener, re-create the span adding that listener along the default one
        val clickableSpans = strBuilder.getSpans(0, spannedHtml.length, ClickableSpan::class.java)
        clickableSpans.forEach { span ->
            addLinkListener(strBuilder, span, callback)
        }
    }

    text = strBuilder
}

private fun addLinkListener(strBuilder: SpannableStringBuilder, span: ClickableSpan, onLinkClicked: OnLinkClicked) {
    val newSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            // Call the original onClick function for the span
            span.onClick(widget)
            // Also call the custom callback function we were given
            widget.let { onLinkClicked(this) }
        }
    }

    with(strBuilder) {
        setSpan(newSpan, getSpanStart(span), getSpanEnd(span), getSpanFlags(span))
        removeSpan(span)
    }
}

/**
 * Programmatically set the drawable tint (android:drawableTint isn't supported in xml until API 23)
 */
fun TextView.setDrawableColor(@ColorRes colorRes: Int) {
    compoundDrawables.filterNotNull().forEach {
        it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_IN)
    }
}
