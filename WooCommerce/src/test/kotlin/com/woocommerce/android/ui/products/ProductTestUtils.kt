package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import org.intellij.lang.annotations.Language
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import java.sql.Date
import java.time.Instant

object ProductTestUtils {

    @Language("JSON")
    private val DEFAULT_PRODUCT_ATTRIBUTES =
        """
            [
              {
                "id": 1,
                "name":"Color",
                "position":"0",
                "visible":"true",
                "variation":"true",
                "options": ["Blue","Green","Red"]
              }
            ]
        """.trimIndent()

    @Language("JSON")
    private val DOWNLOADS =
        """
            [
              {
                "id": 1,
                "name": "test",
                "file": "https://testurl"
              }
            ]
        """.trimIndent()

    fun generateProduct(
        productId: Long = 1L,
        parentID: Long = 0L,
        isVirtual: Boolean = false,
        isVariable: Boolean = false,
        isPurchasable: Boolean = true,
        isDownloadable: Boolean = true,
        customStatus: String? = null,
        variationIds: String = if (isVariable) "[123]" else "[]",
        productType: String? = null,
        amount: String = "20.00",
        productName: String = "product $productId",
        imageUrl: String? = null,
        isStockManaged: Boolean = false,
        productCombinesVariationQuantities: Boolean = false,
        productAttributes: String = DEFAULT_PRODUCT_ATTRIBUTES
    ): Product {
        return generateWCProductModel(
            productId = productId,
            parentID = parentID,
            isVirtual = isVirtual,
            isVariable = isVariable,
            isPurchasable = isPurchasable,
            isDownloadable = isDownloadable,
            customStatus = customStatus,
            variationIds = variationIds,
            productType = productType,
            amount = amount,
            productName = productName,
            imageUrl = imageUrl,
            isStockManaged = isStockManaged,
            productCombinesVariationQuantities = productCombinesVariationQuantities,
            productAttributes = productAttributes
        ).toAppModel()
    }

    fun generateWCProductModel(
        productId: Long = 1L,
        parentID: Long = 0L,
        isVirtual: Boolean = false,
        isVariable: Boolean = false,
        isPurchasable: Boolean = true,
        isDownloadable: Boolean = true,
        customStatus: String? = null,
        variationIds: String = if (isVariable) "[123]" else "[]",
        productType: String? = null,
        amount: String = "20.00",
        productName: String = "product $productId",
        imageUrl: String? = null,
        isStockManaged: Boolean = false,
        productCombinesVariationQuantities: Boolean = false,
        productAttributes: String = DEFAULT_PRODUCT_ATTRIBUTES
    ): WCProductModel {
        return WCProductModel(
            dateCreated = "2018-01-05T05:14:30Z",
            localSiteId = LocalId(2),
            remoteId = RemoteId(productId),
            parentId = parentID,
            status = customStatus ?: "publish",
            type = productType ?: if (isVariable) "variable" else "simple",
            stockStatus = "instock",
            price = amount,
            salePrice = "10.00",
            regularPrice = "30.00",
            averageRating = "3.0",
            name = productName,
            description = "product 1 description",
            images = if (imageUrl != null) """[{"src":"$imageUrl"}]""" else "[]",
            downloadable = isDownloadable,
            downloads = DOWNLOADS,
            weight = "10",
            length = "1",
            width = "2",
            height = "3",
            variations = variationIds,
            attributes = productAttributes,
            categories = "",
            ratingCount = 4,
            groupedProductIds = "[10,11]",
            shortDescription = "short desc",
            virtual = isVirtual,
            stockQuantity = 4.2,
            purchasable = isPurchasable,
            manageStock = isStockManaged,
            combineVariationQuantities = productCombinesVariationQuantities,
            permalink = "https://example.com/product/1",
        )
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

    fun generateProductVariation(
        productId: Long = 1L,
        variationId: Long = 1L,
        amount: String = "10.00",
        isVirtual: Boolean = false,
        isDownloadable: Boolean = false,
        isPurchasable: Boolean = true,
        productAttributes: String = "",
    ): ProductVariation {
        return WCProductVariationModel(LocalId(2)).copy(
            dateCreated = "2018-01-05T05:14:30Z",
            localSiteId = LocalId(1),
            remoteProductId = RemoteId(productId),
            remoteVariationId = RemoteId(variationId),
            price = amount,
            image = "",
            attributes = productAttributes,
            virtual = isVirtual,
            downloadable = isDownloadable,
            purchasable = isPurchasable,
        ).toAppModel().also { it.priceWithCurrency = "$10.00" }
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

    private fun generateTags(): List<ProductTag> {
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
            alt = null,
            source = "Image $imageId source",
            dateCreated = Date.from(Instant.EPOCH),
            isCoverImage = false
        )

    fun generateProductImagesList() =
        (1L..10L).map { id -> generateProductImage(imageId = id) }

    private fun generateProductAttribute(id: Long): ProductAttribute {
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
