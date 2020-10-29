package com.woocommerce.android.ui.products.tags

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.tags.ProductTagsAdapter.OnProductTagClickListener
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.add_product_tag_view.view.*
import java.util.ArrayList

class AddProductTagView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.add_product_tag_view, this)
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
        selectedTagsGroup.getSelectedChip(selectedTag.name)?.let { chip ->
            val anim = WooAnimUtils.getScaleOutAnim(chip)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    selectedTagsGroup.removeView(chip)
                }
            })
            anim.start()
        }
    }

    fun setOnEditorActionListener(cb: (text: String) -> Boolean) {
        addTagsEditText.setOnEditorActionListener(cb)
    }

    fun setOnEditorTextChangedListener(cb: (text: Editable?) -> Unit) {
        addTagsEditText.setOnTextChangedListener(cb)
    }

    fun getEnteredTag() = addTagsEditText.getText()

    private fun addTag(
        tag: ProductTag,
        listener: OnProductTagClickListener,
        animate: Boolean = false
    ) {
        val selectedChipIds = selectedTagsGroup.getSelectedChipIds()
        if (!selectedChipIds.contains(tag.name)) {
            val chip = Chip(context).apply {
                text = tag.name
                this.tag = tag.name
                isCloseIconVisible = true
                isCheckable = false
                isClickable = false
                setOnCloseIconClickListener { listener.onProductTagRemoved(tag) }
            }
            if (animate) {
                val anim = WooAnimUtils.getScaleInAnim(chip)
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        selectedTagsGroup.removeView(chip)
                    }
                })
                anim.start()
            } else {
                selectedTagsGroup.addView(chip)
            }
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
