package com.woocommerce.android.util

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.model.UiDimen
import com.woocommerce.android.model.UiDimen.UiDimenDPInt
import com.woocommerce.android.model.UiDimen.UiDimenRes
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import org.wordpress.android.util.DisplayUtils

object UiHelpers {
    fun getPxOfUiDimen(context: Context, uiDimen: UiDimen): Int =
            when (uiDimen) {
                is UiDimenRes -> context.resources.getDimensionPixelSize(uiDimen.dimenRes)
                is UiDimenDPInt -> DisplayUtils.dpToPx(context, uiDimen.dimensionDP)
            }

    fun getTextOfUiString(context: Context, uiString: UiString): String =
            when (uiString) {
                is UiStringRes -> context.getString(uiString.stringRes)
                is UiStringText -> uiString.text
            }

    fun updateVisibility(view: View, visible: Boolean, setInvisible: Boolean = false) {
        view.visibility = if (visible) {
            View.VISIBLE
        } else {
            if (setInvisible) View.INVISIBLE else View.GONE
        }
    }

    fun setTextOrHide(view: TextView, uiString: UiString?, fmtArgs: String? = null) {
        val text = uiString?.let { getTextOfUiString(view.context, uiString) }
        fmtArgs?.let { args ->
            val message = HtmlCompat.fromHtml(String.format(text!!, args), HtmlCompat.FROM_HTML_MODE_LEGACY)
            setTextOrHide(view, message)
        } ?: setTextOrHide(view, text)
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

    fun setImageOrHide(imageView: ImageView, @DrawableRes resId: Int?, setInvisible: Boolean = false) {
        val isLandscape = DisplayUtils.isLandscape(imageView.context)
        updateVisibility(imageView, resId != null && !isLandscape, setInvisible)
        resId?.let {
            imageView.setImageResource(resId)
        }
    }

    fun setDrawableOrHide(imageView: ImageView, image: Drawable?) {
        updateVisibility(imageView, image != null)
        image?.let { imageView.setImageDrawable(image) }
    }

    fun adjustDialogSize(dialog: Dialog) {
        dialog.window?.let { window ->
            val size = Point()

            val display = window.windowManager.defaultDisplay
            display.getSize(size)

            val width = size.x

            val maximumWidth = window.context.resources.getDimension(R.dimen.alert_dialog_max_width).toInt()
            var proposedWidth = (width * 0.8).toInt()

            if (proposedWidth > maximumWidth) {
                proposedWidth = maximumWidth
            }

            window.setLayout(proposedWidth, LayoutParams.WRAP_CONTENT)
        }
    }
}
