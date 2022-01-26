package com.woocommerce.android.ui.products

import com.woocommerce.android.model.*
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.MediaStore
import java.sql.Date
import java.time.Instant

object ProductTestUtils {
    fun generateProduct(
        productId: Long = 1L,
        isVirtual: Boolean = false,
        isVariable: Boolean = false,
        isPurchasable: Boolean = true,
        customStatus: String? = null
    ): Product {
        return WCProductModel(2).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = productId
            status = customStatus ?: "publish"
            type = "simple"
            stockStatus = "instock"
            price = "20.00"
            salePrice = "10.00"
            regularPrice = "30.00"
            averageRating = "3.0"
            name = "product 1"
            description = "product 1 description"
            images = "[]"
            downloadable = true
            downloads = """[
                                {
                                    "id": 1,
                                    "name": "test",
                                    "file": "https://testurl"
                                }
                            ]"""
            weight = "10"
            length = "1"
            width = "2"
            height = "3"
            variations = if (isVariable) "[123]" else "[]"
            attributes = "[]"
            categories = ""
            ratingCount = 4
            groupedProductIds = "[10,11]"
            ratingCount = 4
            shortDescription = "short desc"
            virtual = isVirtual
            stockQuantity = 4.2
            purchasable = isPurchasable
        }.toAppModel()
    }

    fun generateProductWithTagsAndCategories(productId: Long = 1L): Product {
        return generateProduct(productId).copy(categories = generateProductCategories(), tags = generateTags())
    }

    fun generateProductList(): List<Product> {
        with(ArrayList<Product>()) {
            add(generateProduct(1))
            add(generateProduct(2))
            add(generateProduct(3))
            add(generateProduct(4))
            add(generateProduct(5))
            return this
        }
    }

    fun generateProductListWithDrafts(): List<Product> =
        generateProductList()
            .toMutableList()
            .apply {
                add(generateProduct(6, customStatus = DRAFT.toString()))
            }

    fun generateProductListWithNonPurchasable(): List<Product> =
        generateProductList()
            .toMutableList()
            .apply {
                add(generateProduct(6, isPurchasable = false))
            }

    fun generateProductListWithVariations(): List<Product> =
        generateProductList()
            .toMutableList()
            .apply { add(generateProduct(6, isVariable = true)) }

    private fun generateProductVariation(
        productId: Long = 1L,
        variationId: Long = 1L
    ): ProductVariation {
        return WCProductVariationModel(2).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = productId
            remoteVariationId = variationId
            price = "10.00"
            image = ""
            attributes = ""
        }.toAppModel().also { it.priceWithCurrency = "$10.00" }
    }

    fun generateProductVariationList(productId: Long = 1L): List<ProductVariation> {
        with(ArrayList<ProductVariation>()) {
            add(generateProductVariation(productId, 1))
            add(generateProductVariation(productId, 2))
            add(generateProductVariation(productId, 3))
            add(generateProductVariation(productId, 4))
            add(generateProductVariation(productId, 5))
            return this
        }
    }

    fun generateTags(): List<ProductTag> {
        return listOf(ProductTag(1, "Tag", "Slug", "Desc"))
    }

    fun generateProductCategories(): List<ProductCategory> {
        return mutableListOf<ProductCategory>().apply {
            add(ProductCategory(1, "A", "a", 0))
            add(ProductCategory(2, "B", "b", 0))
            add(ProductCategory(3, "C", "c", 0))
            add(ProductCategory(4, "CA", "ca", 3))
            add(ProductCategory(5, "CAA", "caa", 3))
            add(ProductCategory(6, "CACA", "caca", 4))
            add(ProductCategory(7, "BA", "ba", 2))
            add(ProductCategory(8, "b", "b1", 0))
            add(ProductCategory(9, "c", "c1", 0))
            add(ProductCategory(10, "ca", "ca1", 9))
            add(ProductCategory(11, "ba", "ba1", 8))
        }
    }

    private fun generateProductImage(imageId: Long = 1L) =
        Product.Image(
            id = imageId,
            name = "Image $imageId",
            source = "Image $imageId source",
            dateCreated = Date.from(Instant.EPOCH)
        )

    fun generateProductMedia(remoteProductId: Long = 1, siteId: Long = 1) =
        MediaModel().apply {
            id = 1
            localPostId = remoteProductId.toInt()
            localSiteId = siteId.toInt()
            mediaId = 1L
            fileName = "Image filename $remoteProductId"
            url = "google.com"
        }

    fun generateMediaUploadErrorModel() = MediaStore.MediaError(
        MediaStore.MediaErrorType.GENERIC_ERROR,
        "Error uploading media"
    )

    fun generateProductImagesList() =
        (1L..10L).map { id -> generateProductImage(imageId = id) }

    fun generateProductAttribute(id: Long): ProductAttribute {
        return ProductAttribute(
            id = id,
            name = "attribute$id",
            isVariation = true,
            isVisible = true,
            terms = ArrayList<String>().also {
                it.add("one")
                it.add("two")
                it.add("three")
            }
        )
    }

    fun generateProductAttributeList(): List<ProductAttribute> {
        with(ArrayList<ProductAttribute>()) {
            add(generateProductAttribute(1))
            add(generateProductAttribute(2))
            add(generateProductAttribute(3))
            add(generateProductAttribute(4))
            add(generateProductAttribute(5))
            return this
        }
    }
}
