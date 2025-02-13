package org.wordpress.android.fluxc.wc.product

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductCategoryApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductReviewApiResponse
import kotlin.random.Random

object ProductTestUtils {
    fun generateSampleProduct(
        remoteId: Long,
        type: String = "simple",
        name: String = "",
        virtual: Boolean = false,
        siteId: Int = 6,
        stockStatus: String = CoreProductStockStatus.IN_STOCK.value,
        status: String = "publish",
        stockQuantity: Double = 0.0,
        categories: String = "",
        description: String = "",
        shortDescription: String = "",
    ): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = remoteId
            localSiteId = siteId
            this.type = type
            this.name = name
            this.virtual = virtual
            this.stockStatus = stockStatus
            this.status = status
            this.stockQuantity = stockQuantity
            this.categories = categories
            this.description = description
            this.shortDescription = shortDescription
        }
    }

    fun generateSampleVariation(
        remoteId: Long,
        variationId: Long,
        siteId: Int = 6,
        status: String = "publish",
        stockQuantity: Double = 0.0
    ): WCProductVariationModel {
        return WCProductVariationModel().apply {
            remoteProductId = remoteId
            remoteVariationId = variationId
            localSiteId = siteId
            this.status = status
            this.stockQuantity = stockQuantity
        }
    }

    fun generateSampleVariations(number: Int, productId: Long, siteId: Int): List<WCProductVariationModel> {
        return (0 until number).map {
            generateSampleVariation(
                remoteId = productId,
                variationId = Random.nextLong(),
                siteId = siteId,
                stockQuantity = Random.nextDouble()
            )
        }
    }

    fun generateSampleProductShippingClass(
        remoteId: Long = 1L,
        name: String = "",
        slug: String = "",
        description: String = "",
        siteId: Int = 6
    ): WCProductShippingClassModel {
        return WCProductShippingClassModel().apply {
            remoteShippingClassId = remoteId
            localSiteId = siteId
            this.name = name
            this.slug = slug
            this.description = description
        }
    }

    fun generateSampleProductReview(
        remoteProductId: Long = 1L,
        remoteProductReviewId: Long = 2L,
        siteId: Int = 6
    ): WCProductReviewModel {
        return WCProductReviewModel(1).apply {
            this.remoteProductId = remoteProductId
            this.remoteProductReviewId = remoteProductReviewId
            this.localSiteId = siteId
        }
    }

    fun generateProductShippingClassList(siteId: Int = 6) = List(5) {
        generateSampleProductShippingClass(it + 1L, siteId = siteId)
    }

    fun generateProductList(siteId: Int = 6) = List(5) {
        generateSampleProduct(it + 1L, siteId = siteId)
    }

    fun getProductReviewsFromJsonString(json: String, siteId: Int): List<WCProductReviewModel> {
        val responseType = object : TypeToken<List<ProductReviewApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<ProductReviewApiResponse> ?: emptyList()
        return converted.map {
            WCProductReviewModel().apply {
                localSiteId = siteId
                remoteProductReviewId = it.id
                remoteProductId = it.product_id
                dateCreated = it.date_created_gmt?.let { "${it}Z" } ?: ""
                status = it.status ?: ""
                reviewerName = it.reviewer ?: ""
                reviewerEmail = it.reviewer_email ?: ""
                review = it.review ?: ""
                rating = it.rating
                verified = it.verified
                reviewerAvatarsJson = it.reviewer_avatar_urls.toString()
            }
        }
    }

    fun generateCategory(siteId: Int, remoteId: Long) =
        WCProductCategoryModel().apply {
            localSiteId = siteId
            remoteCategoryId = remoteId
            name = "Category $remoteId"
            slug = "category$remoteId"
            parent = 0L
        }

    fun generateCategoryList(siteId: Int): List<WCProductCategoryModel> {
        return List(5) {
            WCProductCategoryModel().apply {
                localSiteId = siteId
                remoteCategoryId = it.toLong()
                name = "Category $it"
                slug = "category$it"
                parent = 0L
            }
        }
    }

    fun getProductCategories(siteId: Int): List<WCProductCategoryModel> {
        val categoryJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/product-categories.json")
        val responseType = object : TypeToken<List<ProductCategoryApiResponse>>() {}.type
        val converted = Gson().fromJson(categoryJson, responseType) as? List<ProductCategoryApiResponse> ?: emptyList()
        return converted.map {
            WCProductCategoryModel().apply {
                localSiteId = siteId
                remoteCategoryId = it.id
                name = it.name ?: ""
                slug = it.slug ?: ""
                parent = it.parent ?: 0L
            }
        }
    }

    fun generateSampleProductTag(
        remoteId: Long = 1L,
        name: String = "",
        slug: String = "",
        description: String = "",
        count: Int = 3,
        siteId: Int = 6
    ): WCProductTagModel {
        return WCProductTagModel().apply {
            remoteTagId = remoteId
            localSiteId = siteId
            this.name = name
            this.slug = slug
            this.description = description
            this.count = count
        }
    }

    fun generateProductTags(siteId: Int = 6): List<WCProductTagModel> {
        val tagList = mutableListOf<WCProductTagModel>()
        for (i in 0 until 5) {
            tagList.add(generateSampleProductTag(
                    i.toLong(),
                    siteId = siteId,
                    name = "$i",
                    slug = "$i",
                    description = "$i"
            ))
        }
        return tagList
    }
}
