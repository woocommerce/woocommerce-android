package com.woocommerce.android.ui.products.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductDetailUiMapperTest {
    private val mapper = ProductDetailUiMapper()

    @Test
    fun `given offline cache miss, when None is mapped without a product, then terminal empty state is returned`() {
        val result = mapper.mapScreenState(
            auxiliaryState = AuxiliaryState.None,
            hasProduct = false,
            cards = emptyList(),
            showAddMore = false,
            showLinkedProductPromo = false,
        )

        assertThat(result).isEqualTo(ProductDetailScreenState.Empty())
    }

    @Test
    fun `when cards are mapped, then card and property order is preserved`() {
        val cards = listOf(
            ProductPropertyCard(
                type = ProductPropertyCard.Type.PRIMARY,
                properties = listOf(
                    ProductProperty.Editable(R.string.product_detail_title_hint, "Title"),
                    ProductProperty.ComplexProperty(value = "Description"),
                ),
            ),
            ProductPropertyCard(
                type = ProductPropertyCard.Type.SECONDARY,
                caption = "Details",
                properties = allPropertyVariants(),
            ),
        )

        val result = mapper.map(cards)

        assertThat(result.map { it.style }).containsExactly(
            ProductDetailCardStyle.PRIMARY,
            ProductDetailCardStyle.SECONDARY,
        )
        assertThat(result[1].caption).isEqualTo("Details")
        assertThat(result[1].rows.map { it::class.java }).containsExactly(
            ProductDetailRowUiModel.Divider::class.java,
            ProductDetailRowUiModel.Property::class.java,
            ProductDetailRowUiModel.ComplexProperty::class.java,
            ProductDetailRowUiModel.Rating::class.java,
            ProductDetailRowUiModel.Editable::class.java,
            ProductDetailRowUiModel.PropertyGroup::class.java,
            ProductDetailRowUiModel.Link::class.java,
            ProductDetailRowUiModel.Button::class.java,
            ProductDetailRowUiModel.Switch::class.java,
            ProductDetailRowUiModel.Warning::class.java,
        )
    }

    @Test
    fun `when properties are mapped, then values callbacks and ordered groups are preserved`() {
        val callbackResults = mutableListOf<String>()
        val properties = allPropertyVariants(onCallback = callbackResults::add)

        val rows = mapper.map(
            listOf(ProductPropertyCard(ProductPropertyCard.Type.SECONDARY, properties = properties))
        ).single().rows

        val group = rows.filterIsInstance<ProductDetailRowUiModel.PropertyGroup>().single()
        assertThat(group.properties).containsExactly(
            ProductDetailPropertyValueUiModel("First", "1"),
            ProductDetailPropertyValueUiModel("Second", "2"),
        )
        val editable = rows.filterIsInstance<ProductDetailRowUiModel.Editable>().single()
        assertThat(editable.shouldFocus).isTrue()
        assertThat(editable.isReadOnly).isTrue()
        editable.onTextChanged?.invoke("updated")
        rows.filterIsInstance<ProductDetailRowUiModel.ComplexProperty>().single().onClick?.invoke()
        rows.filterIsInstance<ProductDetailRowUiModel.Rating>().single().onClick?.invoke()
        group.onClick?.invoke()
        rows.filterIsInstance<ProductDetailRowUiModel.Link>().single().onClick?.invoke()
        val button = rows.filterIsInstance<ProductDetailRowUiModel.Button>().single()
        button.tooltip?.onDismiss?.invoke()
        button.link?.onClick?.invoke()
        rows.filterIsInstance<ProductDetailRowUiModel.Switch>().single().onStateChanged?.invoke(false)
        button.onClick()

        assertThat(callbackResults).containsExactly(
            "updated",
            "complex",
            "rating",
            "group",
            "link",
            "tooltip",
            "buttonLink",
            "switch",
            "button",
        )
    }

    @Test
    fun `when mutable editable flags change after mapping, then mapped values remain a snapshot`() {
        val editable = ProductProperty.Editable(
            hint = R.string.product_detail_title_hint,
            shouldFocus = true,
            isReadOnly = true,
        )

        val mapped = mapper.map(
            listOf(
                ProductPropertyCard(
                    ProductPropertyCard.Type.PRIMARY,
                    properties = listOf(editable),
                )
            )
        ).single().rows.single() as ProductDetailRowUiModel.Editable
        editable.shouldFocus = false
        editable.isReadOnly = false

        assertThat(mapped.shouldFocus).isTrue()
        assertThat(mapped.isReadOnly).isTrue()
    }

    @Test
    fun `when rating position is mapped, then its legacy divider is hidden only at the end of a card`() {
        val rating = ProductProperty.RatingBar(
            title = R.string.product_reviews,
            value = "4 reviews",
            rating = 4.5f,
            icon = R.drawable.ic_reviews,
        )
        val followedRating = mapper.map(
            listOf(
                ProductPropertyCard(
                    ProductPropertyCard.Type.SECONDARY,
                    properties = listOf(rating, ProductProperty.Warning("Warning")),
                )
            )
        ).single().rows.first() as ProductDetailRowUiModel.Rating
        val finalRating = mapper.map(
            listOf(ProductPropertyCard(ProductPropertyCard.Type.SECONDARY, properties = listOf(rating)))
        ).single().rows.single() as ProductDetailRowUiModel.Rating

        assertThat(followedRating.showDivider).isTrue()
        assertThat(finalRating.showDivider).isFalse()
    }

    @Test
    fun `when semantic rows repeat, then keys are stable and unique without list indexes`() {
        val properties = listOf(
            ProductProperty.Property(R.string.product_price, "10"),
            ProductProperty.Property(R.string.product_price, "20"),
        )

        val first = mapper.map(
            listOf(ProductPropertyCard(ProductPropertyCard.Type.SECONDARY, properties = properties))
        )
        val second = mapper.map(
            listOf(ProductPropertyCard(ProductPropertyCard.Type.SECONDARY, properties = properties))
        )

        assertThat(first.single().rows.map { it.key }).containsExactly(
            "property_${R.string.product_price}",
            "property_${R.string.product_price}_1",
        )
        assertThat(second.single().rows.map { it.key }).isEqualTo(first.single().rows.map { it.key })
    }

    @Test
    fun `given Add is persisted, when cards are remapped, then semantic keys stay stable and callbacks are current`() {
        var callback = ""
        val initialCards = listOf(
            ProductPropertyCard(
                ProductPropertyCard.Type.PRIMARY,
                properties = listOf(
                    ProductProperty.Editable(
                        hint = R.string.product_detail_title_hint,
                        onTextChanged = { callback = "initial:$it" },
                    )
                ),
            )
        )
        val persistedCards = listOf(
            ProductPropertyCard(
                ProductPropertyCard.Type.PRIMARY,
                properties = listOf(
                    ProductProperty.Editable(
                        hint = R.string.product_detail_title_hint,
                        onTextChanged = { callback = "persisted:$it" },
                    )
                ),
            )
        )

        val initial = mapper.map(initialCards)
        val persisted = mapper.map(persistedCards)
        val editable = persisted.single().rows.single() as ProductDetailRowUiModel.Editable
        editable.onTextChanged?.invoke("Title")

        assertThat(persisted.map { it.key }).isEqualTo(initial.map { it.key })
        assertThat(persisted.single().rows.map { it.key }).isEqualTo(initial.single().rows.map { it.key })
        assertThat(callback).isEqualTo("persisted:Title")
    }

    private fun allPropertyVariants(
        onCallback: (String) -> Unit = {},
    ) = listOf(
        ProductProperty.Divider,
        ProductProperty.Property(R.string.product_price, "10"),
        ProductProperty.ComplexProperty(
            title = R.string.product_description,
            value = "Description",
            icon = R.drawable.ic_gridicons_product,
            onClick = { onCallback("complex") },
        ),
        ProductProperty.RatingBar(
            title = R.string.product_reviews,
            value = "4 reviews",
            rating = 4.5f,
            icon = R.drawable.ic_reviews,
            onClick = { onCallback("rating") },
        ),
        ProductProperty.Editable(
            hint = R.string.product_detail_title_hint,
            text = "Title",
            shouldFocus = true,
            isReadOnly = true,
            badgeText = R.string.product_status_private,
            badgeColor = R.color.product_status_badge_pending,
            onTextChanged = onCallback,
        ),
        ProductProperty.PropertyGroup(
            title = R.string.product_inventory,
            properties = linkedMapOf("First" to "1", "Second" to "2"),
            icon = R.drawable.ic_gridicons_list_checkmark,
            isHighlighted = true,
            onClick = { onCallback("group") },
        ),
        ProductProperty.Link(
            title = R.string.product_detail_add_more,
            icon = R.drawable.ic_add,
            onClick = { onCallback("link") },
        ),
        ProductProperty.Button(
            text = R.string.set_up_now,
            icon = R.drawable.ic_add,
            tooltip = ProductProperty.Button.Tooltip(
                title = R.string.tip,
                text = R.string.promo_linked_products_banner_message,
                dismissButtonText = R.string.dismiss,
                onDismiss = { onCallback("tooltip") },
            ),
            link = ProductProperty.Button.Link(R.string.learn_more) { onCallback("buttonLink") },
            onClick = { onCallback("button") },
        ),
        ProductProperty.Switch(
            title = R.string.product_reviews,
            isOn = true,
            icon = R.drawable.ic_reviews,
            onStateChanged = { onCallback("switch") },
        ),
        ProductProperty.Warning("Warning"),
    )
}
