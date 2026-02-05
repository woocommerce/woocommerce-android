package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import javax.inject.Inject

class WooPosLocalCatalogSyncWithFts @Inject constructor(
    private val ftsDao: WooPosSearchableFtsDao,
    private val productsDao: WooPosProductsDao,
    private val variationsDao: WooPosVariationsDao,
    private val featureFlagRepository: FeatureFlagRepository,
    private val gson: Gson,
    private val logger: WooPosLogWrapper,
) {
    private suspend fun isFtsEnabled() = featureFlagRepository.isEnabled(FeatureFlag.POS_PRODUCTS_FTS)

    suspend fun syncFtsForFullSync(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>
    ) {
        if (!isFtsEnabled()) {
            logger.d("syncFtsForFullSync: FTS is disabled, skipping")
            return
        }
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
    }

    suspend fun syncFtsForIncrementalSync(
        siteIdString: String,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>,
        productsToRemove: List<RemoteId>
    ) {
        if (!isFtsEnabled()) {
            logger.d("syncFtsForIncrementalSync: FTS is disabled, skipping")
            return
        }
        if (products.isEmpty() && variations.isEmpty() && productsToRemove.isEmpty()) {
            logger.d("syncFtsForIncrementalSync: no items to update, skipping")
            return
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
    }

    suspend fun ensureFtsPopulated(site: SiteModel) {
        logger.d("ensureFtsPopulated called")
        if (!isFtsEnabled()) {
            logger.d("FTS is disabled, skipping")
            return
        }

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
        val productFtsEntities = products.map { it.toFtsEntity(siteIdString) }

        val productNamesMap = products.associate { it.remoteId.value to it.name }.toMutableMap()

        val missingParentIds = variations
            .map { it.remoteProductId }
            .filter { it.value !in productNamesMap.keys }
            .distinct()

        if (missingParentIds.isNotEmpty() && variations.isNotEmpty()) {
            val localSiteId = variations.first().localSiteId
            val parentProducts = productsDao.getProductsByIds(localSiteId, missingParentIds)
            parentProducts.forEach { productNamesMap[it.remoteId.value] = it.name }
        }

        val variationFtsEntities = variations.mapNotNull { variation ->
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
        val productFtsEntities = products.map { product ->
            product.toFtsEntity(siteIdString)
        }

        val productNamesMap = products.associate { it.remoteId.value to it.name }

        val variationFtsEntities = variations.mapNotNull { variation ->
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

    private data class AttributeJson(
        val id: Long? = null,
        val name: String? = null,
        val option: String? = null
    )
}
