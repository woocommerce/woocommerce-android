package com.woocommerce.android.helpers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * Custom matcher class to match drawables for ImageView
 */
open class DrawableMatcher(resourceId: Int) : TypeSafeMatcher<View>() {
    private var expectedId = 0

    init {
        expectedId = resourceId
    }

    override fun describeTo(description: Description?) {
        description?.appendText("with drawable from resource id: ")
        description?.appendValue(expectedId)
    }

    override fun matchesSafely(item: View?): Boolean {
        // Type check we need to do in TypeSafeMatcher
        if (!(item != null && item is ImageView)) {
            return false
        }

        // We fetch the image view from the focused view
        if (expectedId < 0) {
            return item.drawable == null
        }

        // We get the drawable from the resources that we are going to compare with image view source
        val resources = item.context?.resources
        val expectedDrawable = resources?.getDrawable(expectedId) ?: return false

        // comparing the bitmaps should give results of the matcher if they are equal
        val bitmap = getBitmap(item.drawable)
        val otherBitmap = getBitmap(expectedDrawable)
        return bitmap.sameAs(otherBitmap)
    }

    private fun getBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
