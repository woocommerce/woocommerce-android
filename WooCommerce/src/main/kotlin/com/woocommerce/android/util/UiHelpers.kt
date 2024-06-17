package com.woocommerce.android.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.model.UiDimen
import com.woocommerce.android.model.UiDimen.UiDimenDPInt
import com.woocommerce.android.model.UiDimen.UiDimenRes
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

object UiHelpers {
    fun getPxOfUiDimen(context: Context, uiDimen: UiDimen): Int =
        when (uiDimen) {
            is UiDimenRes -> context.resources.getDimensionPixelSize(uiDimen.dimenRes)
            is UiDimenDPInt -> DisplayUtils.dpToPx(context, uiDimen.dimensionDP)
        }

    fun getTextOfUiString(context: Context, uiString: UiString): String =
        when (uiString) {
            is UiStringText -> uiString.text
            is UiStringRes -> context.getString(
                uiString.stringRes,
                *uiString.params.map { value ->
                    getTextOfUiString(
                        context,
                        value
                    )
                }.toTypedArray()
            )
        }

    fun updateVisibility(view: View, visible: Boolean, setInvisible: Boolean = false) {
        view.visibility = if (visible) {
            View.VISIBLE
        } else {
            if (setInvisible) View.INVISIBLE else View.GONE
        }
    }

    fun setTextOrHide(view: TextView, uiString: UiString?) {
        val message = uiString?.let {
            val pureText = getTextOfUiString(view.context, it)
            if (it.containsHtml) {
                HtmlCompat.fromHtml(pureText, HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                pureText
            }
        }
        setTextOrHide(view, message)
    }

    fun setTextOrHide(view: TextView, @StringRes resId: Int?) {
        val text = resId?.let { view.context.getString(resId) }
        setTextOrHide(view, text)
    }

    fun setTextOrHide(view: TextView, text: CharSequence?) {
        updateVisibility(view, text != null)
        text?.let {
            view.text = text
        }
    }

    fun setImageOrHideInLandscapeOnNonExpandedScreenSizes(
        imageView: ImageView,
        @DrawableRes resId: Int?,
        setInvisible: Boolean = false
    ) {
        val isLandscape = DisplayUtils.isLandscape(imageView.context)
        val isExpandedOrBigger = imageView.context.windowSizeClass == WindowSizeClass.ExpandedAndBigger
        val shouldShowBasedOnOrientationAndSize = !isLandscape || isExpandedOrBigger
        val showImage = resId != null && shouldShowBasedOnOrientationAndSize
        updateVisibility(imageView, showImage, setInvisible)
        resId?.let {
            imageView.setImageResource(resId)
        }
    }

    fun setDrawableOrHide(imageView: ImageView, image: Drawable?) {
        updateVisibility(imageView, image != null)
        image?.let { imageView.setImageDrawable(image) }
    }
}

class IsWindowClassLargeThanCompact @Inject constructor(val context: Context) {
    operator fun invoke() = context.windowSizeClass != WindowSizeClass.Compact
}

class IsWindowClassExpandedAndBigger @Inject constructor(val context: Context) {
    operator fun invoke() = context.windowSizeClass == WindowSizeClass.ExpandedAndBigger
}
