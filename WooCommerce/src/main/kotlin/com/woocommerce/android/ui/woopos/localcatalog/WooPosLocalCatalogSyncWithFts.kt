package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class WooPosLocalCatalogSyncWithFts @Inject constructor(
    private val ftsDao: WooPosSearchableFtsDao,
    private val productsDao: WooPosProductsDao,
    private val variationsDao: WooPosVariationsDao,
    private val filterConfig: WooPosProductsTypesFilterConfig,
    private val gson: Gson,
    private val logger: WooPosLogWrapper,
) {
    data class FtsSyncResult(
        val durationMs: Long,
        val productsIndexed: Int,
    )

    suspend fun syncFtsForFullSync(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>
    ): FtsSyncResult? {
        val startTime = System.currentTimeMillis()
        logger.d("syncFtsForFullSync: clearing and rebuilding FTS index")

        ftsDao.deleteAllForSite(siteIdString)

        val ftsEntities = buildFtsEntities(siteIdString, products, variations)
        if (ftsEntities.isNotEmpty()) {
            ftsDao.insertAll(ftsEntities)
        }

        val duration = System.currentTimeMillis() - startTime
        logger.d(
            "syncFtsForFullSync completed: ${products.size} products, " +
                "${variations.size} variations. Duration: ${duration}ms"
        )

        return FtsSyncResult(
            durationMs = duration,
            productsIndexed = ftsEntities.size,
        )
    }

    suspend fun syncFtsForIncrementalSync(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>,
        productsToRemove: List<RemoteId>
    ): FtsSyncResult? {
        if (products.isEmpty() && variations.isEmpty() && productsToRemove.isEmpty()) {
            logger.d("syncFtsForIncrementalSync: no items to update, skipping")
            return null
        }
        val startTime = System.currentTimeMillis()
        logger.d(
            "syncFtsForIncrementalSync: updating ${products.size} products, " +
                "${variations.size} variations, removing ${productsToRemove.size} products"
        )

        if (products.isNotEmpty()) {
            val productIds = products.map { it.remoteId.value.toString() }
            ftsDao.deleteProducts(siteIdString, productIds)
        }

        if (productsToRemove.isNotEmpty()) {
            val productIdsToRemove = productsToRemove.map { it.value.toString() }
            ftsDao.deleteProducts(siteIdString, productIdsToRemove)
            ftsDao.deleteVariationsByParentProductIds(siteIdString, productIdsToRemove)
        }

        if (variations.isNotEmpty()) {
            val variationIds = variations.map { it.remoteVariationId.value.toString() }
            ftsDao.deleteVariations(siteIdString, variationIds)
        }

        val ftsEntities = buildFtsEntitiesForIncremental(siteIdString, products, variations)
        if (ftsEntities.isNotEmpty()) {
            ftsDao.insertAll(ftsEntities)
        }

        val duration = System.currentTimeMillis() - startTime
        logger.d("syncFtsForIncrementalSync completed. Duration: ${duration}ms")

        return FtsSyncResult(
            durationMs = duration,
            productsIndexed = ftsEntities.size,
        )
    }

    suspend fun ensureFtsPopulated(site: SiteModel) {
        logger.d("ensureFtsPopulated called")

        val siteId = site.localId()
        val siteIdString = siteId.value.toString()

        val productCount = productsDao.getProductCount(siteId)
        if (productCount > 0 && isFtsTableEmpty(siteIdString)) {
            logger.d("FTS table empty with $productCount products, rebuilding index")
            rebuildFtsIndexFromDb(site)
        } else {
            logger.d("FTS already populated or no products to index")
        }
    }

    private suspend fun rebuildFtsIndexFromDb(site: SiteModel) {
        val siteId = site.localId()
        val siteIdString = siteId.value.toString()
        val products = productsDao.getAllProducts(siteId)
        val variations = variationsDao.getAllVariations(siteId)
        syncFtsForFullSync(siteIdString, products, variations)
    }

    private suspend fun isFtsTableEmpty(siteId: String): Boolean {
        return ftsDao.countAllForSite(siteId) == 0
    }

    private suspend fun buildFtsEntitiesForIncremental(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>
    ): List<WooPosSearchableFtsEntity> {
        val eligibleProducts = products
            .filter { it.isEligibleForFts() }
            .distinctBy { it.remoteId }
        val eligibleVariations = variations
            .filter { it.isEligibleForFts() }
            .distinctBy { it.remoteVariationId }

        val productFtsEntities = eligibleProducts.map { it.toFtsEntity(siteIdString) }

        val productNamesMap = eligibleProducts.associate { it.remoteId.value to it.name }.toMutableMap()

        val missingParentIds = eligibleVariations
            .map { it.remoteProductId }
            .filter { it.value !in productNamesMap.keys }
            .distinct()

        if (missingParentIds.isNotEmpty() && eligibleVariations.isNotEmpty()) {
            val localSiteId = eligibleVariations.first().localSiteId
            val parentProducts = productsDao.getProductsByIds(localSiteId, missingParentIds)
            parentProducts.forEach { productNamesMap[it.remoteId.value] = it.name }
        }

        val variationFtsEntities = eligibleVariations.mapNotNull { variation ->
            val parentName = productNamesMap[variation.remoteProductId.value]
            if (parentName == null) {
                logger.w(
                    "Skipping variation ${variation.remoteVariationId.value}: " +
                        "parent product ${variation.remoteProductId.value} not found"
                )
                return@mapNotNull null
            }
            variation.toFtsEntity(siteIdString, parentName)
        }

        return productFtsEntities + variationFtsEntities
    }

    private fun buildFtsEntities(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>
    ): List<WooPosSearchableFtsEntity> {
        val eligibleProducts = products
            .filter { it.isEligibleForFts() }
            .distinctBy { it.remoteId }
        val eligibleVariations = variations
            .filter { it.isEligibleForFts() }
            .distinctBy { it.remoteVariationId }

        val productFtsEntities = eligibleProducts.map { product ->
            product.toFtsEntity(siteIdString)
        }

        val productNamesMap = eligibleProducts.associate { it.remoteId.value to it.name }

        val variationFtsEntities = eligibleVariations.mapNotNull { variation ->
            val parentName = productNamesMap[variation.remoteProductId.value]
            if (parentName == null) {
                logger.w(
                    "Skipping variation ${variation.remoteVariationId.value}: " +
                        "parent product ${variation.remoteProductId.value} not found"
                )
                return@mapNotNull null
            }
            variation.toFtsEntity(siteIdString, parentName)
        }

        return productFtsEntities + variationFtsEntities
    }

    private fun WooPosProductEntity.toFtsEntity(siteIdString: String) = WooPosSearchableFtsEntity(
        localSiteId = siteIdString,
        itemId = remoteId.value.toString(),
        parentProductId = "",
        name = name,
        sku = sku,
        barcode = globalUniqueId,
        attributeValues = ""
    )

    private fun WooPosVariationEntity.toFtsEntity(
        siteIdString: String,
        parentProductName: String
    ) = WooPosSearchableFtsEntity(
        localSiteId = siteIdString,
        itemId = remoteVariationId.value.toString(),
        parentProductId = remoteProductId.value.toString(),
        name = parentProductName,
        sku = sku,
        barcode = globalUniqueId,
        attributeValues = extractAttributeValues(attributesJson)
    )

    private fun extractAttributeValues(attributesJson: String): String {
        if (attributesJson.isBlank() || attributesJson == "[]") {
            return ""
        }

        return try {
            val type = object : TypeToken<List<AttributeJson>>() {}.type
            val attributes: List<AttributeJson> = gson.fromJson(attributesJson, type)
            attributes
                .mapNotNull { it.option?.takeIf { option -> option.isNotBlank() } }
                .joinToString(" ")
        } catch (e: JsonSyntaxException) {
            WooLog.e(WooLog.T.POS, "Failed to parse attributes JSON for FTS: $attributesJson", e)
            ""
        }
    }

    private val allowedStatus = filterConfig.filters[ProductFilterOption.STATUS]
    private val allowedTypes = filterConfig.includeTypes.map { it.value }.toSet()
    private val allowedDownloadable =
        filterConfig.filters[ProductFilterOption.DOWNLOADABLE]?.toBoolean() ?: false

    private fun WooPosProductEntity.isEligibleForFts(): Boolean =
        status == allowedStatus && type in allowedTypes && downloadable == allowedDownloadable

    private fun WooPosVariationEntity.isEligibleForFts(): Boolean =
        status == allowedStatus && downloadable == allowedDownloadable

    private data class AttributeJson(
        val id: Long? = null,
        val name: String? = null,
        val option: String? = null
    )
}
