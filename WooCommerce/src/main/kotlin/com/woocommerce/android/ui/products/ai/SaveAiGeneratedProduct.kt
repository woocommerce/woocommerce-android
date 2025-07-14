package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.model.Image
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ai.preview.UploadImage
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class SaveAiGeneratedProduct @Inject constructor(
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val productTagsRepository: ProductTagsRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val uploadImage: UploadImage
) {
    suspend operator fun invoke(
        product: Product,
        selectedImage: Image?
    ): AiProductSaveResult = coroutineScope {
        // Start uploading the selected image
        val imageTask = selectedImage?.let { selectedImage -> startUploadingImage(selectedImage) }

        val missingCategories = product.categories.filter { it.remoteCategoryId == 0L }
        // Start create missing categories
        val categoriesTask = missingCategories
            .takeIf { it.isNotEmpty() }
            ?.let { productCategories -> startCreatingCategories(productCategories) }

        // Start Create missing tags
        val missingTags = product.tags.filter { it.remoteTagId == 0L }
        val tagsTask = missingTags
            .takeIf { it.isNotEmpty() }
            ?.let { productTags -> startCreatingTags(productTags) }

        // Wait for the image to be uploaded
        val image = imageTask?.await()?.getOrElse {
            return@coroutineScope AiProductSaveResult.Failure.UploadImageFailure
        }

        // Wait for the created categories and tags
        val createdCategories = categoriesTask?.await()?.getOrElse {
            return@coroutineScope AiProductSaveResult.Failure.Generic(image?.asWPMediaLibraryImage())
        }
        val createdTags = tagsTask?.await()?.getOrElse {
            return@coroutineScope AiProductSaveResult.Failure.Generic(image?.asWPMediaLibraryImage())
        }

        val updatedProduct = product.copy(
            categories = product.categories - missingCategories.toSet() + createdCategories.orEmpty(),
            tags = product.tags - missingTags.toSet() + createdTags.orEmpty(),
            images = listOfNotNull(image),
            status = ProductStatus.DRAFT
        )

        productDetailRepository.addProduct(updatedProduct).let { (success, productId) ->
            if (success) {
                WooLog.d(
                    tag = WooLog.T.PRODUCTS,
                    message = "Successfully saved the AI generated product as draft with id $productId"
                )
                AiProductSaveResult.Success(productId)
            } else {
                WooLog.e(WooLog.T.PRODUCTS, "Failed to save the AI generated product as draft")
                AiProductSaveResult.Failure.Generic(image?.asWPMediaLibraryImage())
            }
        }
    }

    private fun CoroutineScope.startUploadingImage(image: Image) = async {
        uploadImage(image).onFailure {
            WooLog.e(WooLog.T.PRODUCTS, "Failed to upload the selected image", it)
        }
    }

    private fun CoroutineScope.startCreatingCategories(categories: List<ProductCategory>) = async {
        WooLog.d(
            tag = WooLog.T.PRODUCTS,
            message = "Create the missing product categories ${categories.map { it.name }}"
        )
        productCategoriesRepository.addProductCategories(categories)
            .onFailure {
                WooLog.e(WooLog.T.PRODUCTS, "Failed to add product categories", it)
            }
    }

    private fun CoroutineScope.startCreatingTags(tags: List<ProductTag>) = async {
        WooLog.d(
            tag = WooLog.T.PRODUCTS,
            message = "Create the missing product tags ${tags.map { it.name }}"
        )
        productTagsRepository.addProductTags(tags.map { it.name })
            .onFailure {
                WooLog.e(WooLog.T.PRODUCTS, "Failed to add product tags", it)
            }
    }

    private fun Product.Image.asWPMediaLibraryImage() = Image.WPMediaLibraryImage(this)
}

sealed interface AiProductSaveResult {
    data class Success(val productId: Long) : AiProductSaveResult
    sealed interface Failure : AiProductSaveResult {
        data object UploadImageFailure : Failure
        data class Generic(val uploadedImage: Image.WPMediaLibraryImage? = null) : Failure
    }
}
