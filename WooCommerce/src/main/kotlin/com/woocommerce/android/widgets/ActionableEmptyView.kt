package com.woocommerce.android.widgets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActionableEmptyViewBinding
import com.woocommerce.android.util.WooAnimUtils
import org.wordpress.android.util.DisplayUtils

class ActionableEmptyView : LinearLayout {
    private val binding = ActionableEmptyViewBinding.inflate(LayoutInflater.from(context), this, true)

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

        attrs.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ActionableEmptyView, 0, 0)

            val imageResource = typedArray.getResourceId(R.styleable.ActionableEmptyView_aevImage, 0)
            val titleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevTitle)
            val buttonAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevButton)
            val titleAppearance = typedArray.getResourceId(R.styleable.ActionableEmptyView_aevTitleAppearance, 0)

            if (imageResource != 0) {
                binding.emptyViewImage.setImageResource(imageResource)
                binding.emptyViewImage.visibility = View.VISIBLE
            }

            if (titleAppearance != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.emptyViewText.setTextAppearance(titleAppearance)
                } else {
                    binding.emptyViewText.setTextAppearance(context, titleAppearance)
                }
            }

            if (!titleAttribute.isNullOrEmpty()) {
                binding.emptyViewText.text = titleAttribute
            } else {
                throw RuntimeException("$context: ActionableEmptyView must have a title (aevTitle)")
            }

            if (!buttonAttribute.isNullOrEmpty()) {
                binding.emptyViewButton.text = buttonAttribute
                binding.emptyViewButton.visibility = View.VISIBLE
            }

            typedArray.recycle()
        }

        checkOrientation()
    }

    fun updateVisibility(shouldBeVisible: Boolean, showButton: Boolean) {
        if (shouldBeVisible && isVisible.not()) {
            WooAnimUtils.fadeIn(this)
            showButton(showButton)
        } else if (shouldBeVisible.not() && isVisible) {
            WooAnimUtils.fadeOut(this)
        }
    }

    fun showButton(show: Boolean) {
        binding.emptyViewButton.isVisible = show
    }

    fun setOnClickListener(action: (View) -> Unit) {
        binding.emptyViewButton.setOnClickListener(action)
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
            binding.root.setPadding(0, context.resources.getDimensionPixelSize(R.dimen.major_300), 0, 0)

            binding.emptyViewImage.visibility = View.GONE
            binding.emptyViewButton.visibility = View.GONE
        } else {
            params = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            binding.root.setPadding(0, 0, 0, 0)
        }

        params.topMargin = topMargin
        binding.root.layoutParams = params
    }

    /**
     * Hide the main image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        binding.emptyViewImage.visibility = if (binding.emptyViewImage.visibility == View.VISIBLE && !isLandscape) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
