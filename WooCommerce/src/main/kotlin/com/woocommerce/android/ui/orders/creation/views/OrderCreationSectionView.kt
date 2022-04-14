package com.woocommerce.android.ui.orders.creation.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.use
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationSectionBinding

class OrderCreationSectionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderCreationSectionBinding.inflate(LayoutInflater.from(ctx), this, true)

    var isEachAddButtonEnabled
        get() = binding.addButtonsLayout.children.all { it.isEnabled }
        set(value) {
            binding.addButtonsLayout.children.forEach {
                it.isEnabled = value
            }
        }

    var header: CharSequence
        get() = binding.headerLabel.text
        set(value) {
            binding.headerLabel.text = value
        }

    var content: View?
        get() = binding.contentLayout.children.firstOrNull()
        set(value) {
            updateContent(value)
        }

    var keepAddButtons: Boolean = false
    var hasEditButton: Boolean = true

    init {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.OrderCreationSectionView, defStyleAttr, 0)
                .use { a ->
                    header = a.getString(R.styleable.OrderCreationSectionView_header).orEmpty()
                    keepAddButtons = a.getBoolean(R.styleable.OrderCreationSectionView_keepAddButtons, keepAddButtons)
                    hasEditButton = a.getBoolean(R.styleable.OrderCreationSectionView_hasEditButton, hasEditButton)
                }
        }
    }

    fun setAddButtons(buttons: List<AddButton>) {
        binding.addButtonsLayout.removeAllViews()
        buttons.forEach { buttonModel ->
            val button = MaterialButton(context, null, R.attr.secondaryTextButtonStyle)
            button.text = buttonModel.text
            button.icon = AppCompatResources.getDrawable(context, R.drawable.ic_add)
            button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            button.setOnClickListener { buttonModel.onClickListener() }
            binding.addButtonsLayout.addView(
                button,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun updateContent(content: View?) {
        binding.editButton.isVisible = content != null && hasEditButton
        binding.contentLayout.isVisible = content != null
        binding.contentLayout.removeAllViews()
        content?.let {
            binding.contentLayout.addView(
                content,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        binding.addButtonsLayout.isVisible = keepAddButtons || content == null
    }

    fun setContentHorizontalPadding(@DimenRes padding: Int) {
        val paddingSize = context.resources.getDimensionPixelSize(padding)
        binding.contentLayout.setPadding(
            paddingSize,
            binding.contentLayout.paddingTop,
            paddingSize,
            binding.contentLayout.paddingBottom
        )
    }

    fun setOnEditButtonClicked(listener: () -> Unit) {
        binding.editButton.setOnClickListener { listener() }
    }

    fun setEditButtonContentDescription(contentDescription: String) {
        binding.editButton.contentDescription = contentDescription
    }

    data class AddButton(
        val text: CharSequence,
        val onClickListener: () -> Unit
    )
}
