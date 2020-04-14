package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.woocommerce.android.R

open class WCElevatedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var shapeElevation = context.resources.getDimensionPixelSize(R.dimen.plane_02).toFloat()
    private var elevatedBackground: MaterialShapeDrawable

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCElevatedLinearLayout)
            try {
                shapeElevation = attrArray.getDimension(R.styleable.WCElevatedLinearLayout_wcElevation, shapeElevation)
            } finally {
                attrArray.recycle()
            }
        }

        elevatedBackground = MaterialShapeDrawable.createWithElevationOverlay(context, shapeElevation)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        background = elevatedBackground
    }

    override fun setElevation(elevation: Float) {
        elevatedBackground.elevation = elevation
    }
}
