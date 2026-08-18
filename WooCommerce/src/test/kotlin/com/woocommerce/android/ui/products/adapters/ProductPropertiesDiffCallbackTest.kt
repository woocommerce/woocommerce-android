package com.woocommerce.android.ui.products.adapters

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductPropertiesDiffCallbackTest {
    @Test
    fun `given an empty editable, when text arrives, then immutable focus payload is returned`() {
        // GIVEN
        val oldItem = editable(text = "")
        val newItem = editable(text = "Title")
        val callback = ProductPropertiesDiffCallback(listOf(oldItem), listOf(newItem))

        // WHEN
        val payload = callback.getChangePayload(0, 0)
        val focusedItem = newItem.withEditableFocus(listOf(EditableFocusPayload))

        // THEN
        assertThat(callback.areContentsTheSame(0, 0)).isFalse()
        assertThat(payload).isSameAs(EditableFocusPayload)
        assertThat(newItem.shouldFocus).isFalse()
        assertThat(focusedItem).isEqualTo(newItem.copy(shouldFocus = true))
    }

    @Test
    fun `given an editable in progress, when persisted text arrives, then rebinding remains suppressed`() {
        // GIVEN
        val callback = ProductPropertiesDiffCallback(
            oldList = listOf(editable(text = "Local edit")),
            newList = listOf(editable(text = "Persisted edit")),
        )

        // WHEN
        val contentsAreTheSame = callback.areContentsTheSame(0, 0)

        // THEN
        assertThat(contentsAreTheSame).isTrue()
    }

    private fun editable(text: String) = ProductProperty.Editable(
        hint = R.string.product_detail_title_hint,
        text = text,
    )
}
