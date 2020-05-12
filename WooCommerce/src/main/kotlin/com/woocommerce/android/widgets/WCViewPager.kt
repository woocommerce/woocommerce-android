package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

/**
 * Simple ViewPager wrapped design to address the common "pointer index out of bounds" exception in
 * the native ViewPager.
 *
 * https://github.com/woocommerce/woocommerce-android/issues/1729
 */
class WCViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.onTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            WooLog.e(T.PRODUCTS, e)
            false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            WooLog.e(T.PRODUCTS, e)
            false
        }
    }
}
