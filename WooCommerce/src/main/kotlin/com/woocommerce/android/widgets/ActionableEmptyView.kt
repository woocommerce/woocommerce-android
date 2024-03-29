package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
                binding.emptyViewText.setTextAppearance(titleAppearance)
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
