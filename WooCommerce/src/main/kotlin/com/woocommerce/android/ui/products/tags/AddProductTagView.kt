package com.woocommerce.android.ui.products.tags

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.tags.ProductTagsAdapter.OnProductTagClickListener
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
        val selectedChip = selectedTagsGroup.getSelectedChip(selectedTag.remoteTagId.toInt())
        selectedChip?.let { selectedTagsGroup.removeView(it) }
    }

    fun hasTags() = selectedTagsGroup.childCount > 0

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        addTagsEditText.doAfterTextChanged {
            cb(it)
        }
    }

    private fun addTag(
        tag: ProductTag,
        listener: OnProductTagClickListener
    ) {
        val selectedChipIds = selectedTagsGroup.getSelectedChipIds()
        if (!selectedChipIds.contains(tag.remoteTagId.toInt())) {
            val chip = Chip(context).apply {
                text = tag.name
                id = tag.remoteTagId.toInt()
                isCloseIconVisible = true
                isCheckable = false
                isClickable = false
                setOnCloseIconClickListener { listener.onProductTagRemoved(tag) }
            }
            selectedTagsGroup.addView(chip)
        }
    }

    private fun ChipGroup.getSelectedChipIds(): MutableList<Int> {
        val checkedIds = ArrayList<Int>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Chip) {
                checkedIds.add(child.getId())
            }
        }
        return checkedIds
    }

    private fun ChipGroup.getSelectedChip(id: Int): Chip? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Chip && child.id == id) {
                return child
            }
        }
        return null
    }
}
