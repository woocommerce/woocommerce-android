package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.IsPosProductsFtsEnabled
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogSyncWithFts @Inject constructor(
    private val ftsDao: WooPosSearchableFtsDao,
    private val productsDao: WooPosProductsDao,
    private val variationsDao: WooPosVariationsDao,
    private val isFtsEnabled: IsPosProductsFtsEnabled,
    private val gson: Gson,
    private val logger: WooPosLogWrapper,
) {
    suspend fun populateFtsAfterFullSync(site: SiteModel) {
        if (!isFtsEnabled()) return
        rebuildFtsIndex(site)
    }

    suspend fun updateFtsAfterIncrementalSync(site: SiteModel) {
        if (!isFtsEnabled()) return
        rebuildFtsIndex(site)
    }

    suspend fun ensureFtsPopulated(site: SiteModel) {
        if (!isFtsEnabled()) return

        val siteId = site.localId()
        val siteIdString = siteId.value.toString()

        val productCount = productsDao.getProductCount(siteId)
        if (productCount > 0 && isFtsTableEmpty(siteIdString)) {
            rebuildFtsIndex(site)
        }
    }

    private suspend fun rebuildFtsIndex(site: SiteModel) {
        val startTime = System.currentTimeMillis()
        logger.d("Starting FTS index rebuild")

        val siteId = site.localId()
        val siteIdString = siteId.value.toString()

        ftsDao.deleteAllForSite(siteIdString)

        val products = productsDao.getAllProducts(siteId)
        val variations = variationsDao.getAllVariations(siteId)

        val ftsEntities = buildFtsEntities(siteIdString, products, variations)

        if (ftsEntities.isNotEmpty()) {
            ftsDao.insertAll(ftsEntities)
        }

        val duration = System.currentTimeMillis() - startTime
        logger.d(
            "FTS index rebuild completed: ${products.size} products, " +
                "${variations.size} variations. Duration: ${duration}ms"
        )
    }

    private suspend fun isFtsTableEmpty(siteId: String): Boolean {
        return ftsDao.countAllForSite(siteId) == 0
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

        val variationFtsEntities = variations.map { variation ->
            val parentName = productNamesMap[variation.remoteProductId.value] ?: ""
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
