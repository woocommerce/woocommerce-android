package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.Ignore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.utils.getBoolean
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * Single Woo product - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-properties
 * Note that products have more properties than we support below
 */
@Entity(
    tableName = "ProductEntity",
    primaryKeys = ["localSiteId", "remoteId"],
)
data class WCProductModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteId: RemoteId = RemoteId(0),
    val name: String = "",
    val slug: String = "",
    val permalink: String = "",

    val dateCreated: String = "",
    val dateModified: String = "",

    val type: String = "",
    val status: String = "",
    val featured: Boolean = false,
    val catalogVisibility: String = "",
    val description: String = "",
    val shortDescription: String = "",
    val sku: String = "",
    val globalUniqueId: String = "",

    val price: String = "",
    val regularPrice: String = "",
    val salePrice: String = "",
    val onSale: Boolean = false,
    val totalSales: Long = 0L,
    val purchasable: Boolean = false,

    val dateOnSaleFrom: String = "",
    val dateOnSaleTo: String = "",
    val dateOnSaleFromGmt: String = "",
    val dateOnSaleToGmt: String = "",

    val virtual: Boolean = false,
    val downloadable: Boolean = false,
    val downloadLimit: Long = -1L,
    val downloadExpiry: Int = -1,
    val soldIndividually: Boolean = false,

    val externalUrl: String = "",
    val buttonText: String = "",

    val taxStatus: String = "",
    val taxClass: String = "",

    val manageStock: Boolean = false,
    val stockQuantity: Double = 0.0,
    val stockStatus: String = "",

    val backorders: String = "",
    val backordersAllowed: Boolean = false,
    val backordered: Boolean = false,

    val shippingRequired: Boolean = false,
    val shippingTaxable: Boolean = false,
    val shippingClass: String = "",
    val shippingClassId: Int = 0,

    val reviewsAllowed: Boolean = true,
    val averageRating: String = "",
    val ratingCount: Int = 0,

    val parentId: Long = 0L,
    val purchaseNote: String = "",
    val menuOrder: Int = 0,

    val categories: String = "",
    val tags: String = "",
    val images: String = "",
    val attributes: String = "",
    val variations: String = "",
    val downloads: String = "",
    val relatedIds: String = "",
    val crossSellIds: String = "",
    val upsellIds: String = "",
    val groupedProductIds: String = "",

    val weight: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",

    val bundledItems: String = "",
    val compositeComponents: String = "",
    val specialStockStatus: String = "",
    val bundleMinSize: Float? = null,
    val bundleMaxSize: Float? = null,
    val minAllowedQuantity: Int = -1,
    val maxAllowedQuantity: Int = -1,
    val groupOfQuantity: Int = -1,
    val combineVariationQuantities: Boolean = false,
    val password: String? = null,

    val isSampleProduct: Boolean = false
) {

    @Ignore
    val remoteProductId: Long = remoteId.value

    val attributeList: Array<ProductAttribute>
        get() = Gson().fromJson(attributes, Array<ProductAttribute>::class.java) ?: emptyArray()

    val isConfigurable: Boolean
        get() = when (type) {
            CoreProductType.BUNDLE.value -> {
                runCatching { Gson().fromJson(bundledItems, Array<WCBundledProduct>::class.java) }
                    .takeIf { it.isSuccess }?.getOrNull()
                    ?.let { products ->
                        products.any { it.isConfigurable() }
                    } ?: false
            }
            else -> false
        }

    class ProductTriplet(val id: Long, val name: String, val slug: String) {
        fun toJson(): JsonObject {
            return JsonObject().also { json ->
                json.addProperty("id", id)
                json.addProperty("name", name)
                json.addProperty("slug", slug)
            }
        }
    }

    class ProductAttribute(
        val id: Long,
        val name: String,
        val variation: Boolean,
        val visible: Boolean,
        options: List<String>
    ) {
        val options: MutableList<String> = options.toMutableList()

        fun isSameAttribute(other: ProductAttribute): Boolean {
            return id == other.id &&
                    name == other.name &&
                    variation == other.variation &&
                    visible == other.visible &&
                    options == other.options
        }

        fun toJson(): JsonObject {
            val jsonOptions = JsonArray().also {
                for (option in options) {
                    it.add(option)
                }
            }
            return JsonObject().also { json ->
                json.addProperty("id", id)
                json.addProperty("name", name)
                json.addProperty("visible", visible)
                json.addProperty("variation", variation)
                json.add("options", jsonOptions)
            }
        }
    }

    /**
     * Returns true if this product has the same attributes as the passed product
     */
    fun hasSameAttributes(otherProduct: WCProductModel): Boolean {
        // do a quick string comparison first so we can avoid parsing the attributes when possible
        if (this.attributes == otherProduct.attributes) {
            return true
        }

        val otherAttributes = otherProduct.attributeList
        val thisAttributes = this.attributeList
        if (thisAttributes.size != otherAttributes.size) {
            return false
        }

        for (i in thisAttributes.indices) {
            if (!thisAttributes[i].isSameAttribute(otherAttributes[i])) {
                return false
            }
        }

        return true
    }

    /**
     * Parses the images json array into a list of product images
     */
    fun getImageListOrEmpty(): List<WCProductImageModel> {
        return if (images.isNotEmpty()) {
            try {
                val jsonElement = Gson().fromJson(images, JsonElement::class.java)
                getImageList(jsonElement.asJsonArray)
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
                emptyList()
            } catch (e: IllegalStateException) {
                AppLog.e(T.API, e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun getImageList(imagesAsJsonArray: JsonArray): List<WCProductImageModel> {
        val imageList = arrayListOf<WCProductImageModel>()
        imagesAsJsonArray.forEach { jsonElement ->
            with(jsonElement.asJsonObject) {
                WCProductImageModel(this.getLong("id")).also {
                    it.name = this.getString("name") ?: ""
                    it.src = this.getString("src") ?: ""
                    it.alt = this.getString("alt")
                    imageList.add(it)
                }
            }
        }
        return imageList
    }

    /**
     * Extract the first image url from the json array of images
     */
    fun getFirstImageUrl(): String? {
        try {
            if (images.isNotEmpty()) {
                Gson().fromJson(images, JsonElement::class.java).asJsonArray.firstOrNull { jsonElement ->
                    return (jsonElement.asJsonObject).getString("src")
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return null
    }

    /**
     * Returns the list of products attributes. The function returns an empty list
     * when the attributes json deserialization fails.
     */
    fun getAttributeList(): List<ProductAttribute> {
        fun getAttributeOptions(jsonArray: JsonArray?): List<String> {
            val options = ArrayList<String>()
            try {
                jsonArray?.forEach {
                    options.add(it.asString)
                }
            } catch (e: ClassCastException) {
                AppLog.e(T.API, e)
            } catch (e: IllegalStateException) {
                AppLog.e(T.API, e)
            }
            return options
        }

        return kotlin.runCatching {
            Gson().fromJson(attributes, JsonElement::class.java)
                .asJsonArray.asSequence()
                .map { it.asJsonObject }
                .map { json ->
                    ProductAttribute(
                        id = json.getLong("id"),
                        name = json.getString("name") ?: "",
                        variation = json.getBoolean("variation", true),
                        visible = json.getBoolean("visible", true),
                        options = getAttributeOptions(json.getAsJsonArray("options"))
                    )
                }.toList()
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                AppLog.e(T.API, e)
                emptyList()
            }
        )
    }

    fun getDownloadableFiles(): List<WCProductFileModel> {
        if (downloads.isEmpty()) return emptyList()
        val fileList = ArrayList<WCProductFileModel>()
        try {
            Gson().fromJson(downloads, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                with(jsonElement.asJsonObject) {
                    fileList.add(
                            WCProductFileModel(
                                    id = this.getString("id"),
                                    name = this.getString("name") ?: "",
                                    url = this.getString("file") ?: ""
                            )
                    )
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return fileList
    }

    /**
     * Returns a list of product IDs from the passed string, assumed to be a JSON array of IDs
     *
     * Deserializes the [jsonString] passed to the method. This can include:
     * variations, groupedProductIds, upsellIds, crossSellIds
     *
     * There are some instances where the [jsonString] param can be a JsonArray or a JsonElement.
     * https://github.com/woocommerce/woocommerce-android/issues/2374
     *
     * And there are some instances where the [jsonString] can be a JsonArray of JsonObjects
     * https://github.com/woocommerce/woocommerce-android/issues/6737
     *
     * To address the above issues, we check if the [jsonString] param is a JsonArray or
     * a JsonElement, then we check each item if it's a JsonPrimitive or JsonObject and return
     * an appropriate response, if that's the case.
     */
    private fun parseJson(jsonString: String): List<Long> {
        return if (jsonString.isNotEmpty()) {
            try {
                val jsonElement = Gson().fromJson(jsonString, JsonElement::class.java)
                parseJsonIfNotEmpty(jsonElement)
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun parseJsonIfNotEmpty(jsonElement: JsonElement): List<Long> {
        fun JsonElement.parseId() = when {
            isJsonObject -> asJsonObject["id"]?.asLong
                ?: throw JsonParseException("Can't extract element's ID")
            isJsonPrimitive -> asLong
            else -> throw JsonParseException("Can't extract element's ID")
        }

        val ids = arrayListOf<Long>()
        return when {
            jsonElement.isJsonNull -> emptyList()
            jsonElement.isJsonArray -> {
                jsonElement.asJsonArray.forEach { element -> element.parseId().let { ids.add(it) } }
                ids
            }
            jsonElement.isJsonObject -> {
                jsonElement.asJsonObject.entrySet().forEach { ids.add(it.value.parseId()) }
                ids
            }
            else -> emptyList()
        }
    }

    fun getNumVariations() = getVariationIdList().size

    fun getVariationIdList() = parseJson(variations)

    fun getGroupedProductIdList() = parseJson(groupedProductIds)

    fun getUpsellProductIdList() = parseJson(upsellIds)

    fun getCrossSellProductIdList() = parseJson(crossSellIds)

    fun getCategoryList() = getTripletsOrEmpty(categories)

    fun getTagList() = getTripletsOrEmpty(tags)

    private fun getTripletsOrEmpty(jsonStr: String): List<ProductTriplet> {
        return if (jsonStr.isNotEmpty()) {
            try {
                val jsonElement = Gson().fromJson(jsonStr, JsonElement::class.java)
                getTriplets(jsonElement)
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun getTriplets(tripletsAsJsonElement: JsonElement): List<ProductTriplet> {
        val triplets = arrayListOf<ProductTriplet>()
        if (tripletsAsJsonElement.isJsonArray) {
            tripletsAsJsonElement.asJsonArray.forEach { jsonArray ->
                with(jsonArray.asJsonObject) {
                    triplets.add(
                        ProductTriplet(
                            id = this.getLong("id"),
                            name = this.getString("name") ?: "",
                            slug = this.getString("slug") ?: ""
                        )
                    )
                }
            }
        }
        return triplets
    }

    /**
     * Compares this product's images with the passed product's images, returns true only if both
     * lists contain the same images in the same order with the same alt text and name
     */
    fun hasSameImages(updatedProduct: WCProductModel): Boolean {
        val updatedImages = updatedProduct.getImageListOrEmpty()
        val thisImages = getImageListOrEmpty()
        if (thisImages.size != updatedImages.size) {
            return false
        }
        for (i in thisImages.indices) {
            if (thisImages[i].id != updatedImages[i].id ||
                thisImages[i].alt != updatedImages[i].alt ||
                thisImages[i].name != updatedImages[i].name
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's categories with the passed product's categories, returns true only if both
     * lists contain the same categories in the same order
     */
    fun hasSameCategories(updatedProduct: WCProductModel): Boolean {
        val updatedCategories = updatedProduct.getCategoryList()
        val storedCategories = getCategoryList()
        if (storedCategories.size != updatedCategories.size) {
            return false
        }
        for (i in storedCategories.indices) {
            if (storedCategories[i].id != updatedCategories[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's tags with the passed product's tags, returns true only if both
     * lists contain the same tags in the same order
     */
    fun hasSameTags(updatedProduct: WCProductModel): Boolean {
        val updatedTags = updatedProduct.getTagList()
        val storedTags = getTagList()
        if (storedTags.size != updatedTags.size) {
            return false
        }
        for (i in storedTags.indices) {
            if (storedTags[i].id != updatedTags[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's downloadable files with the passed product's files, returns true only if both
     * lists contain the same set of files in the same order
     */
    fun hasSameDownloadableFiles(updatedProduct: WCProductModel): Boolean {
        val updatedFiles = updatedProduct.getDownloadableFiles()
        val storedFiles = getDownloadableFiles()
        return storedFiles == updatedFiles
    }

    object SubscriptionMetadataKeys {
        const val SUBSCRIPTION_PRICE = "_subscription_price"
        const val SUBSCRIPTION_PERIOD = "_subscription_period"
        const val SUBSCRIPTION_PERIOD_INTERVAL = "_subscription_period_interval"
        const val SUBSCRIPTION_LENGTH = "_subscription_length"
        const val SUBSCRIPTION_SIGN_UP_FEE = "_subscription_sign_up_fee"
        const val SUBSCRIPTION_TRIAL_PERIOD = "_subscription_trial_period"
        const val SUBSCRIPTION_TRIAL_LENGTH = "_subscription_trial_length"
        const val SUBSCRIPTION_ONE_TIME_SHIPPING = "_subscription_one_time_shipping"
        const val SUBSCRIPTION_PAYMENT_SYNC_DATE = "_subscription_payment_sync_date"
        val ALL_KEYS = setOf(
            SUBSCRIPTION_PRICE,
            SUBSCRIPTION_TRIAL_LENGTH,
            SUBSCRIPTION_SIGN_UP_FEE,
            SUBSCRIPTION_PERIOD,
            SUBSCRIPTION_PERIOD_INTERVAL,
            SUBSCRIPTION_LENGTH,
            SUBSCRIPTION_TRIAL_PERIOD,
            SUBSCRIPTION_ONE_TIME_SHIPPING,
            SUBSCRIPTION_PAYMENT_SYNC_DATE
        )
    }
}
