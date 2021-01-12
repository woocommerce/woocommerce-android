package com.woocommerce.android.ui.products.tags

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.woocommerce.android.databinding.AddProductTagViewBinding
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.tags.ProductTagsAdapter.OnProductTagClickListener
import com.woocommerce.android.util.WooAnimUtils
import java.util.ArrayList

class AddProductTagView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding: AddProductTagViewBinding
    init {
        binding = AddProductTagViewBinding.inflate(LayoutInflater.from(ctx), this)
    }

    fun addSelectedTags(
        selectedTags: List<ProductTag>,
        listener: OnProductTagClickListener
    ) {
        selectedTags.forEach { addTag(it, listener) }
    }

    fun removeSelectedTag(
        selectedTag: ProductTag
    ) {
        binding.selectedTagsGroup.getSelectedChip(selectedTag.name)?.let { chip ->
            val anim = WooAnimUtils.getScaleOutAnim(chip)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.selectedTagsGroup.removeView(chip)
                }
            })
            anim.start()
        }
    }

    fun setOnEditorActionListener(cb: (text: String) -> Boolean) {
        binding.addTagsEditText.setOnEditorActionListener(cb)
    }

    fun setOnEditorTextChangedListener(cb: (text: Editable?) -> Unit) {
        binding.addTagsEditText.setOnTextChangedListener(cb)
    }

    fun getEnteredTag() = binding.addTagsEditText.getText()

    private fun addTag(
        tag: ProductTag,
        listener: OnProductTagClickListener
    ) {
        val selectedChipIds = binding.selectedTagsGroup.getSelectedChipIds()
        if (!selectedChipIds.contains(tag.name)) {
            val chip = Chip(context).apply {
                text = tag.name
                this.tag = tag.name
                isCloseIconVisible = true
                isCheckable = false
                isClickable = false
                visibility = View.INVISIBLE
                setOnCloseIconClickListener { listener.onProductTagRemoved(tag) }
            }

            binding.selectedTagsGroup.addView(chip)
            WooAnimUtils.scaleIn(chip)
        }
    }

    private fun ChipGroup.getSelectedChipIds(): MutableList<String> {
        val checkedIds = ArrayList<String>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Chip) {
                checkedIds.add(child.tag as String)
            }
        }
        return checkedIds
    }

    private fun ChipGroup.getSelectedChip(tag: String): Chip? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Chip && child.tag == tag) {
                return child
            }
        }
        return null
    }
}
