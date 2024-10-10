package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.model.Image
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ai.preview.UploadImage
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class SaveAiGeneratedProduct @Inject constructor(
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val productTagsRepository: ProductTagsRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val uploadImage: UploadImage
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(
        product: Product,
        selectedImage: Image?
    ): Result<Long> {
        // upload the selected image
        val image = selectedImage?.let { uploadImage(it) }?.getOrElse {
            WooLog.e(WooLog.T.PRODUCTS, "Failed to upload the selected image", it)
            return Result.failure(it)
        }

        // Create missing categories
        val missingCategories = product.categories.filter { it.remoteCategoryId == 0L }
        val createdCategories = missingCategories
            .takeIf { it.isNotEmpty() }?.let { productCategories ->
                WooLog.d(
                    tag = WooLog.T.PRODUCTS,
                    message = "Create the missing product categories ${productCategories.map { it.name }}"
                )
                productCategoriesRepository.addProductCategories(productCategories)
            }?.fold(
                onSuccess = { it },
                onFailure = {
                    WooLog.e(WooLog.T.PRODUCTS, "Failed to add product categories", it)
                    return Result.failure(it)
                }
            )

        // Create missing tags
        val missingTags = product.tags.filter { it.remoteTagId == 0L }
        val createdTags = missingTags
            .takeIf { it.isNotEmpty() }?.let { productTags ->
                WooLog.d(
                    tag = WooLog.T.PRODUCTS,
                    message = "Create the missing product tags ${productTags.map { it.name }}"
                )
                productTagsRepository.addProductTags(productTags.map { it.name })
            }?.fold(
                onSuccess = { it },
                onFailure = {
                    WooLog.e(WooLog.T.PRODUCTS, "Failed to add product tags", it)
                    return Result.failure(it)
                }
            )

        val updatedProduct = product.copy(
            categories = product.categories - missingCategories.toSet() + createdCategories.orEmpty(),
            tags = product.tags - missingTags.toSet() + createdTags.orEmpty(),
            images = listOfNotNull(image),
            status = ProductStatus.DRAFT
        )

        return productDetailRepository.addProduct(updatedProduct).let { (success, productId) ->
            if (success) {
                Result.success(productId)
            } else {
                Result.failure(Exception("Failed to save the AI generated product"))
            }
        }
    }
}
