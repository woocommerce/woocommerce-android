package com.woocommerce.android.widgets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.actionable_empty_view.view.*
import org.wordpress.android.util.DisplayUtils

class ActionableEmptyView : LinearLayout {
    lateinit var button: MaterialButton
    lateinit var image: ImageView
    lateinit var layout: View
    lateinit var title: MaterialTextView

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet) {
        clipChildren = false
        clipToPadding = false
        gravity = Gravity.CENTER
        orientation = VERTICAL

        layout = View.inflate(context, R.layout.actionable_empty_view, this)

        image = layout.findViewById(R.id.empty_view_image)
        title = layout.findViewById(R.id.empty_view_text)
        button = layout.findViewById(R.id.empty_view_button)

        attrs.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ActionableEmptyView, 0, 0)

            val imageResource = typedArray.getResourceId(R.styleable.ActionableEmptyView_aevImage, 0)
            val titleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevTitle)
            val buttonAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevButton)
            val titleAppearance = typedArray.getResourceId(R.styleable.ActionableEmptyView_aevTitleAppearance, 0)

            if (imageResource != 0) {
                image.setImageResource(imageResource)
                image.visibility = View.VISIBLE
            }

            if (titleAppearance != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    title.setTextAppearance(titleAppearance)
                } else {
                    title.setTextAppearance(context, titleAppearance)
                }
            }

            if (!titleAttribute.isNullOrEmpty()) {
                title.text = titleAttribute
            } else {
                throw RuntimeException("$context: ActionableEmptyView must have a title (aevTitle)")
            }

            if (!buttonAttribute.isNullOrEmpty()) {
                button.text = buttonAttribute
                button.visibility = View.VISIBLE
            }

            typedArray.recycle()
        }

        checkOrientation()
    }

    /**
     * Update actionable empty view layout when used while searching.  The following characteristics are for each case:
     *      Default - center in parent, use original top margin
     *      Search  - center at top of parent, use original top margin, add 48dp top padding, hide image, hide button
     *
     * @param isSearching true when searching; false otherwise
     * @param topMargin top margin in pixels to offset with other views (e.g. toolbar or tabs)
     */
    fun updateLayoutForSearch(isSearching: Boolean, topMargin: Int) {
        val params: RelativeLayout.LayoutParams

        if (isSearching) {
            params = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            layout.setPadding(0, context.resources.getDimensionPixelSize(R.dimen.major_300), 0, 0)

            image.visibility = View.GONE
            button.visibility = View.GONE
        } else {
            params = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layout.setPadding(0, 0, 0, 0)
        }

        params.topMargin = topMargin
        layout.layoutParams = params
    }

    /**
     * Hide the main image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        empty_view_image.visibility = if (empty_view_image.visibility == View.VISIBLE && !isLandscape) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
