package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.woocommerce.android.R

open class WCElevatedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var shapeElevation = context.resources.getDimension(R.dimen.plane_01)
    private var elevatedBackground: MaterialShapeDrawable

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCElevatedLinearLayout)
            try {
                shapeElevation = attrArray.getDimension(
                    R.styleable.WCElevatedLinearLayout_android_elevation, shapeElevation)
            } finally {
                attrArray.recycle()
            }
        }

        elevatedBackground = MaterialShapeDrawable.createWithElevationOverlay(context, shapeElevation)
        elevatedBackground.shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        background = elevatedBackground
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Automatically don't clip children for the parent view. This allows the shadow
        // to be drawn outside the bounds.
        if (parent is ViewGroup) {
            (parent as ViewGroup).clipChildren = false
        }
    }

    override fun setElevation(elevation: Float) {
        elevatedBackground.elevation = elevation
    }
}
