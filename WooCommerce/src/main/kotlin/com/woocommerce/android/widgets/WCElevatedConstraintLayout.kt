package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.woocommerce.android.R

open class WCElevatedConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var shapeElevation = context.resources.getDimension(R.dimen.plane_01)
    private var elevatedBackground: MaterialShapeDrawable

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCElevatedConstraintLayout)
            try {
                shapeElevation = attrArray.getDimension(
                    R.styleable.WCElevatedConstraintLayout_wcElevation, shapeElevation)
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
