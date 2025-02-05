package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.utils.getString
import javax.inject.Inject

class ProductDtoMapper @Inject constructor(
    private val stripProductMetaData: StripProductMetaData
) {
    @Suppress("LongMethod", "ComplexMethod")
    fun mapToModel(localSiteId: LocalOrRemoteId.LocalId, dto: ProductDto): ProductWithMetaData {
        val fatMetaDataList = dto.metadata?.mapNotNull {
            runCatching { WCMetaData.fromJson(it.asJsonObject) }.getOrNull()
        } ?: emptyList()
        val metaData = stripProductMetaData(fatMetaDataList)

        val model = WCProductModel().apply {
            this.localSiteId = localSiteId.value
            remoteProductId = dto.id ?: 0
            name = dto.name ?: ""
            slug = dto.slug ?: ""
            permalink = dto.permalink ?: ""

            dateCreated = dto.date_created ?: ""
            dateModified = dto.date_modified ?: ""

            dateOnSaleFrom = dto.date_on_sale_from ?: ""
            dateOnSaleTo = dto.date_on_sale_to ?: ""
            dateOnSaleFromGmt = dto.date_on_sale_from_gmt ?: ""
            dateOnSaleToGmt = dto.date_on_sale_to_gmt ?: ""

            type = dto.type ?: ""
            status = dto.status ?: ""
            featured = dto.featured
            catalogVisibility = dto.catalog_visibility ?: ""
            description = dto.description ?: ""
            shortDescription = dto.short_description ?: ""
            sku = dto.sku ?: ""
            globalUniqueId = dto.global_unique_id ?: ""

            price = dto.price ?: ""
            regularPrice = dto.regular_price ?: ""
            salePrice = dto.sale_price ?: ""
            onSale = dto.on_sale
            totalSales = dto.total_sales
            purchasable = dto.purchasable

            virtual = dto.virtual
            downloadable = dto.downloadable
            downloadLimit = dto.download_limit
            downloadExpiry = dto.download_expiry

            externalUrl = dto.external_url ?: ""
            buttonText = dto.button_text ?: ""

            taxStatus = dto.tax_status ?: ""
            taxClass = dto.tax_class ?: ""

            // variations may have "parent" here if inventory is enabled for the parent but not the variation
            manageStock = dto.manage_stock?.let {
                it == "true" || it == "parent"
            } ?: false

            stockQuantity = dto.stock_quantity ?: 0.0

            stockStatus = dto.stock_status ?: ""

            backorders = dto.backorders ?: ""
            backordersAllowed = dto.backorders_allowed
            backordered = dto.backordered
            soldIndividually = dto.sold_individually
            weight = dto.weight ?: ""

            shippingRequired = dto.shipping_required
            shippingTaxable = dto.shipping_taxable
            shippingClass = dto.shipping_class ?: ""
            shippingClassId = dto.shipping_class_id

            reviewsAllowed = dto.reviews_allowed
            averageRating = dto.average_rating ?: ""
            ratingCount = dto.rating_count

            parentId = dto.parent_id
            menuOrder = dto.menu_order
            purchaseNote = dto.purchase_note ?: ""

            categories = dto.categories?.toString() ?: ""
            tags = dto.tags?.toString() ?: ""
            images = dto.images?.toString() ?: ""
            attributes = dto.attributes?.toString() ?: ""
            variations = dto.variations?.toString() ?: ""
            downloads = dto.downloads?.toString() ?: ""
            relatedIds = dto.related_ids?.toString() ?: ""
            crossSellIds = dto.cross_sell_ids?.toString() ?: ""
            upsellIds = dto.upsell_ids?.toString() ?: ""
            groupedProductIds = dto.grouped_products?.toString() ?: ""
            bundledItems = dto.bundled_items?.toString() ?: ""
            compositeComponents = dto.composite_components?.toString() ?: ""
            minAllowedQuantity = dto.min_quantity?.toInt() ?: -1
            maxAllowedQuantity = dto.max_quantity?.let { it.ifEmpty { "0" } }?.toInt() ?: -1
            groupOfQuantity = dto.group_of_quantity?.toInt() ?: -1

            combineVariationQuantities = dto.combine_variations?.let {
                it == "yes"
            } ?: false

            password = dto.password

            isSampleProduct = dto.metadata?.any {
                val metaDataEntry = WCMetaData.fromJson(it.asJsonObject)
                metaDataEntry?.let { json ->
                    json.key == "_headstart_post" && json.valueAsString == "_hs_extra"
                } ?: false
            } ?: false

            dto.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }
        }

        return ProductWithMetaData(
            product = model.applyBundledProductChanges(dto),
            metaData = metaData
        )
    }

    private fun WCProductModel.applyBundledProductChanges(dto: ProductApiResponse) = apply {
        if (this.type == CoreProductType.BUNDLE.value) {
            if (dto.bundle_stock_status !in CoreProductStockStatus.ALL_VALUES) {
                specialStockStatus = dto.bundle_stock_status ?: ""
            }

            bundleMaxSize = dto.bundle_max_size?.toFloatOrNull()
            bundleMinSize = dto.bundle_min_size?.toFloatOrNull()
        }
    }
}
