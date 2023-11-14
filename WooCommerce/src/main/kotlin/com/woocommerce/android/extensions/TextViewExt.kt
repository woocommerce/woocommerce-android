package com.woocommerce.android.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import com.woocommerce.android.widgets.WooClickableSpan

typealias OnLinkClicked = (ClickableSpan) -> Unit

/**
 * Sets the TextView text from a raw HTML string.
 *
 * Optionally, provide an extra [onLinkClicked] callback to be triggered when any of the links in the HTML are clicked.
 * The callback is triggered in addition to the default link handling behavior, it does not override it.
 */
fun TextView.setHtmlText(html: String, onLinkClicked: OnLinkClicked? = null) {
    val spannedHtml = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)

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

/**
 * Makes any text range inside a TextView clickable with a special color and a URL redirection
 */
fun TextView.setClickableText(
    content: String,
    clickableContent: String,
    clickAction: WooClickableSpan
) {
    SpannableString(content).apply {
        setSpan(
            clickAction,
            indexOf(clickableContent),
            indexOf(clickableContent) + clickableContent.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }.let {
        setText(it, TextView.BufferType.SPANNABLE)
        movementMethod = LinkMovementMethod.getInstance()
    }
}

fun TextView.selectAllText() {
    Selection.setSelection(text.toSpannable(), 0, length())
}
