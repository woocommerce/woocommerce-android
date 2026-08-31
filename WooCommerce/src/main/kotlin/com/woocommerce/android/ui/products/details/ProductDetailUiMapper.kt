package com.woocommerce.android.ui.products.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Loading
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.None
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard

class ProductDetailUiMapper {
    fun mapPageState(
        title: String,
        topAppBar: ProductDetailTopAppBarUiState,
        screen: ProductDetailScreenState,
        image: ProductDetailImageUiState,
        hasUploadErrors: Boolean,
    ) = ProductDetailPageUiState(
        title = title,
        topAppBar = topAppBar,
        screen = screen,
        image = image,
        showUploadError = hasUploadErrors &&
            screen !is ProductDetailScreenState.Error &&
            image != ProductDetailImageUiState.Hidden,
    )

    fun mapImageState(
        auxiliaryState: AuxiliaryState,
        hasProduct: Boolean,
        areImagesAvailable: Boolean,
        persistedImages: List<Product.Image>,
        uploadingImageUris: List<String>,
    ): ProductDetailImageUiState = when {
        auxiliaryState is Error -> ProductDetailImageUiState.Hidden
        auxiliaryState == Loading -> ProductDetailImageUiState.Loading
        !hasProduct -> ProductDetailImageUiState.Hidden
        !areImagesAvailable -> ProductDetailImageUiState.Unavailable
        persistedImages.isEmpty() && uploadingImageUris.isEmpty() -> ProductDetailImageUiState.AddImage
        else -> ProductDetailImageUiState.Gallery(
            mapImageItems(
                persistedImages = persistedImages,
                uploadingImageUris = uploadingImageUris,
            )
        )
    }

    fun mapScreenState(
        auxiliaryState: AuxiliaryState,
        hasProduct: Boolean,
        cards: List<ProductDetailCardUiModel>,
        showAddMore: Boolean,
        showLinkedProductPromo: Boolean,
    ): ProductDetailScreenState = when (auxiliaryState) {
        Loading -> ProductDetailScreenState.Loading
        is Error -> if (auxiliaryState.message == R.string.product_detail_product_not_selected) {
            ProductDetailScreenState.Empty(auxiliaryState.message)
        } else {
            ProductDetailScreenState.Error(auxiliaryState.message)
        }
        None -> if (hasProduct) {
            ProductDetailScreenState.Content(
                cards = cards,
                showAddMore = showAddMore,
                showLinkedProductPromo = showLinkedProductPromo,
            )
        } else {
            ProductDetailScreenState.Empty()
        }
    }

    fun map(cards: List<ProductPropertyCard>): List<ProductDetailCardUiModel> {
        val cardKeyOccurrences = mutableMapOf<String, Int>()
        return cards.map { card ->
            val rows = mapRows(card.properties)
            val cardBaseKey = when (card.type) {
                ProductPropertyCard.Type.PRIMARY -> PRIMARY_CARD_KEY
                ProductPropertyCard.Type.SECONDARY -> if (card.properties.any { it.isBlazeProperty() }) {
                    BLAZE_CARD_KEY
                } else {
                    SECONDARY_CARD_KEY
                }
            }
            val cardKey = cardBaseKey.withOccurrence(cardKeyOccurrences)

            ProductDetailCardUiModel(
                key = cardKey,
                style = when (card.type) {
                    ProductPropertyCard.Type.PRIMARY -> ProductDetailCardStyle.PRIMARY
                    ProductPropertyCard.Type.SECONDARY -> ProductDetailCardStyle.SECONDARY
                },
                caption = card.caption,
                rows = rows,
            )
        }
    }

    private fun mapImageItems(
        persistedImages: List<Product.Image>,
        uploadingImageUris: List<String>,
    ): List<ProductDetailImageUiItem> {
        val uploadingKeyOccurrences = mutableMapOf<String, Int>()
        val uploadingItems = uploadingImageUris.map { source ->
            val occurrence = source.nextOccurrence(uploadingKeyOccurrences)
            ProductDetailImageUiItem.Uploading(
                key = "uploading:${source.length}:$source:$occurrence",
                source = source,
            )
        }.asReversed()

        val persistedKeyOccurrences = mutableMapOf<PersistedImageIdentity, Int>()
        val persistedItems = persistedImages.map { image ->
            val identity = PersistedImageIdentity(image.id, image.source)
            val occurrence = identity.nextOccurrence(persistedKeyOccurrences)
            ProductDetailImageUiItem.Persisted(
                key = "persisted:${image.id}:${image.source.length}:${image.source}:$occurrence",
                source = image.source,
                isCover = image.isCoverImage,
            )
        }

        return buildList {
            addAll(uploadingItems)
            addAll(persistedItems)
            if (persistedItems.isNotEmpty()) {
                add(ProductDetailImageUiItem.Add)
            }
        }
    }

    private fun mapRows(properties: List<ProductProperty>): List<ProductDetailRow> {
        val keyOccurrences = mutableMapOf<String, Int>()
        return properties.mapIndexed { index, property ->
            val key = property.semanticKey().withOccurrence(keyOccurrences)
            ProductDetailRow(
                key = key,
                property = property,
                showDivider = (index != properties.lastIndex).takeIf { property is ProductProperty.RatingBar },
            )
        }
    }

    private fun ProductProperty.semanticKey(): String = when (this) {
        ProductProperty.Divider -> "divider"
        is ProductProperty.Property -> "property_$title"
        is ProductProperty.ComplexProperty -> "complex_${title ?: NO_RESOURCE}_${icon ?: NO_RESOURCE}"
        is ProductProperty.RatingBar -> "rating_$title"
        is ProductProperty.Editable -> "editable_$hint"
        is ProductProperty.PropertyGroup -> "group_$title"
        is ProductProperty.Link -> "link_$title"
        is ProductProperty.Button -> "button_$text"
        is ProductProperty.Switch -> "switch_$title"
        is ProductProperty.Warning -> "warning"
    }

    private fun ProductProperty.isBlazeProperty() =
        this is ProductProperty.Link && title == R.string.product_details_blaze_card

    private fun String.withOccurrence(occurrences: MutableMap<String, Int>): String {
        val occurrence = nextOccurrence(occurrences)
        return if (occurrence == 0) this else "${this}_$occurrence"
    }

    private fun <T> T.nextOccurrence(occurrences: MutableMap<T, Int>): Int =
        occurrences.getOrDefault(this, 0).also { occurrences[this] = it + 1 }

    private data class PersistedImageIdentity(val id: Long, val source: String)

    private companion object {
        const val PRIMARY_CARD_KEY = "primary"
        const val SECONDARY_CARD_KEY = "secondary"
        const val BLAZE_CARD_KEY = "blaze"
        const val NO_RESOURCE = 0
    }
}
