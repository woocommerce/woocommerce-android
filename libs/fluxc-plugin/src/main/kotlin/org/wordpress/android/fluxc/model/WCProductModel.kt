package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.utils.getBoolean
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * Single Woo product - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-properties
 * Note that products have more properties than we support below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L // The unique identifier for this product on the server
    val remoteId
        get() = RemoteId(remoteProductId)
    @Column var name = ""
    @Column var slug = ""
    @Column var permalink = ""

    @Column var dateCreated = ""
    @Column var dateModified = ""

    @Column var type = "" // simple, grouped, external, variable
    @Column var status = ""
    @Column var featured = false
    @Column var catalogVisibility = "" // visible, catalog, search, hidden
    @Column var description = ""
    @Column var shortDescription = ""
    @Column var sku = ""
    @Column var globalUniqueId = ""

    @Column var price = ""
    @Column var regularPrice = ""
    @Column var salePrice = ""
    @Column var onSale = false
    @Column var totalSales = 0L
    @Column var purchasable = false

    @Column var dateOnSaleFrom = ""
    @Column var dateOnSaleTo = ""
    @Column var dateOnSaleFromGmt = ""
    @Column var dateOnSaleToGmt = ""

    @Column var virtual = false
    @Column var downloadable = false
    @Column var downloadLimit = -1L
    @Column var downloadExpiry = -1
    @Column var soldIndividually = false

    @Column var externalUrl = ""
    @Column var buttonText = ""

    @Column var taxStatus = "" // taxable, shipping, none
    @Column var taxClass = ""

    @Column var manageStock = false
    @Column var stockQuantity = 0.0
    @Column var stockStatus = "" // instock, outofstock, onbackorder

    @Column var backorders = "" // no, notify, yes
    @Column var backordersAllowed = false
    @Column var backordered = false

    @Column var shippingRequired = false
    @Column var shippingTaxable = false
    @Column var shippingClass = ""
    @Column var shippingClassId = 0

    @Column var reviewsAllowed = true
    @Column var averageRating = ""
    @Column var ratingCount = 0

    @Column var parentId = 0L
    @Column var purchaseNote = ""
    @Column var menuOrder = 0

    @Column var categories = "" // array of categories
    @Column var tags = "" // array of tags
    @Column var images = "" // array of images
    @Column var attributes = "" // array of attributes
    @Column var variations = "" // array of variation IDs
    @Column var downloads = "" // array of downloadable files
    @Column var relatedIds = "" // array of related product IDs
    @Column var crossSellIds = "" // array of cross-sell product IDs
    @Column var upsellIds = "" // array of up-sell product IDs
    @Column var groupedProductIds = "" // array of grouped product IDs

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    @Column var bundledItems = ""
    @Column var compositeComponents = ""
    @Column var specialStockStatus = ""
    @Column var bundleMinSize: Float? = null
    @Column var bundleMaxSize: Float? = null
    @Column var minAllowedQuantity = -1
    @Column var maxAllowedQuantity = -1
    @Column var groupOfQuantity = -1
    @Column var combineVariationQuantities = false
    @Column var password: String? = null

    @Column var isSampleProduct = false
        @JvmName("setIsSampleProduct")
        set

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

        fun getCommaSeparatedOptions(): String {
            if (options.isEmpty()) return ""
            var commaSeparatedOptions = ""
            options.forEach { option ->
                if (commaSeparatedOptions.isEmpty()) {
                    commaSeparatedOptions = option
                } else {
                    commaSeparatedOptions += ", $option"
                }
            }
            return commaSeparatedOptions
        }

        fun isSameAttribute(other: ProductAttribute): Boolean {
            return id == other.id &&
                    name == other.name &&
                    variation == other.variation &&
                    visible == other.visible &&
                    options == other.options
        }

        fun asGlobalAttribute(siteID: Int) =
                WCGlobalAttributeSqlUtils.fetchSingleStoredAttribute(id.toInt(), siteID)

        fun generateProductVariantOption(selectedOption: String) =
                takeIf { options.contains(selectedOption) }?.let {
                    ProductVariantOption(
                            id = id,
                            name = name,
                            option = selectedOption
                    )
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

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    fun addAttribute(newAttribute: ProductAttribute) {
        mutableListOf<ProductAttribute>()
                .apply {
                    attributeList
                            .takeIf {
                                it.find { currentAttribute ->
                                    currentAttribute.id == newAttribute.id
                                } == null
                            }?.let { currentAttributes ->
                                add(newAttribute)
                                currentAttributes
                                        .takeIf { it.isNotEmpty() }
                                        ?.let { addAll(it) }
                            }
                }.also { attributes = Gson().toJson(it) }
    }

    fun removeAttribute(attributeID: Int) =
            mutableListOf<ProductAttribute>().apply {
                attributeList
                        .takeIf { it.isNotEmpty() }
                        ?.filter { attributeID != it.id.toInt() }
                        ?.let { addAll(it) }
            }.also { attributes = Gson().toJson(it) }

    fun getAttribute(attributeID: Int) =
        attributeList.find { it.id == attributeID.toLong() }

    fun updateAttribute(updatedAttribute: ProductAttribute) = apply {
        getAttribute(updatedAttribute.id.toInt())
                ?.let { removeAttribute(it.id.toInt()) }
        addAttribute(updatedAttribute)
    }

    /**
     * Returns true if this product has the same attributes as the passed product
     */
    @Suppress("ReturnCount")
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
                    it.alt = this.getString("alt") ?: ""
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

    fun getCommaSeparatedCategoryNames() = getCommaSeparatedTripletNames(getCategoryList())

    fun getTagList() = getTripletsOrEmpty(tags)

    fun getCommaSeparatedTagNames() = getCommaSeparatedTripletNames(getTagList())

    private fun getCommaSeparatedTripletNames(triplets: List<ProductTriplet>): String {
        if (triplets.isEmpty()) return ""
        var commaSeparatedNames = ""
        triplets.forEach {
            if (commaSeparatedNames.isEmpty()) {
                commaSeparatedNames = it.name
            } else {
                commaSeparatedNames += ", ${it.name}"
            }
        }
        return commaSeparatedNames
    }

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
     * lists contain the same images in the same order
     */
    @Suppress("ReturnCount")
    fun hasSameImages(updatedProduct: WCProductModel): Boolean {
        val updatedImages = updatedProduct.getImageListOrEmpty()
        val thisImages = getImageListOrEmpty()
        if (thisImages.size != updatedImages.size) {
            return false
        }
        for (i in thisImages.indices) {
            if (thisImages[i].id != updatedImages[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's categories with the passed product's categories, returns true only if both
     * lists contain the same categories in the same order
     */
    @Suppress("ReturnCount")
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
    @Suppress("ReturnCount")
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
