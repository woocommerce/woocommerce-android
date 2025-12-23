package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPosVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToWooPOSEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

class WooPosCatalogFileParser @Inject constructor(
    private val logger: WooPosLogWrapper,
    private val gson: Gson,
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        private const val TYPE_FIELD = "type"
        private const val DATA_FIELD = "data"
        private const val TYPE_SIMPLE = "simple"
        private const val TYPE_VARIABLE = "variable"
        private const val TYPE_VARIATION = "variation"
    }

    data class ParsedCatalogData(
        val products: List<WooPosProductEntity>,
        val variations: List<WooPosVariationEntity>
    )

    private sealed class CatalogItem {
        data class Product(val entity: WooPosProductEntity) : CatalogItem()
        data class Variation(val entity: WooPosVariationEntity) : CatalogItem()
        object Unknown : CatalogItem()
    }

    private data class RawCatalogEntry(
        val type: String?,
        val data: JsonElement?
    )

    suspend fun parseCatalogFile(
        file: File,
        localSiteId: LocalOrRemoteId.LocalId
    ): Result<ParsedCatalogData> = withContext(dispatchers.io) {
        runCatching {
            logger.d("Starting to parse catalog file: ${file.absolutePath}")
            if (!file.exists()) {
                throw FileNotFoundException("Catalog file not found: ${file.absolutePath}")
            }

            val catalogItems = file.bufferedReader().use { reader ->
                JsonReader(reader).use { jsonReader ->
                    parseCatalogItems(jsonReader, localSiteId)
                }
            }

            val (products, variations) = catalogItems.partition { it is CatalogItem.Product }
            val productEntities = products.mapNotNull { (it as? CatalogItem.Product)?.entity }
            val variationEntities = variations.mapNotNull { (it as? CatalogItem.Variation)?.entity }
            logger.d("Parsed ${productEntities.size} products and ${variationEntities.size} variations")
            ParsedCatalogData(
                products = productEntities,
                variations = variationEntities
            )
        }.onFailure { exception ->
            logger.e("Failed to parse catalog file: ${exception.message}", exception)
        }
    }

    private fun parseCatalogItems(
        reader: JsonReader,
        localSiteId: LocalOrRemoteId.LocalId
    ): List<CatalogItem> {
        val items = mutableListOf<CatalogItem>()
        reader.beginArray()
        while (reader.hasNext()) {
            val rawEntry = readCatalogEntry(reader)
            val catalogItem = processCatalogEntry(rawEntry, localSiteId)
            if (catalogItem !is CatalogItem.Unknown) {
                items.add(catalogItem)
            }
        }
        reader.endArray()
        return items
    }

    private fun readCatalogEntry(reader: JsonReader): RawCatalogEntry {
        var type: String? = null
        var dataElement: JsonElement? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                TYPE_FIELD -> type = reader.nextString()
                DATA_FIELD -> dataElement = JsonParser.parseReader(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return RawCatalogEntry(type, dataElement)
    }

    private fun processCatalogEntry(
        entry: RawCatalogEntry,
        localSiteId: LocalOrRemoteId.LocalId
    ): CatalogItem {
        val (type, dataElement) = entry
        if (type == null || dataElement == null) {
            return CatalogItem.Unknown
        }

        return try {
            when (type) {
                TYPE_SIMPLE, TYPE_VARIABLE -> {
                    val productResponse = gson.fromJson(dataElement, ProductApiResponse::class.java)
                    val productEntity = productResponse.mapToWooPOSEntity(localSiteId)
                    CatalogItem.Product(productEntity)
                }
                TYPE_VARIATION -> {
                    val variationResponse = gson.fromJson(dataElement, WooPosVariationApiResponse::class.java)
                    val variationEntity = variationResponse.mapToPosVariationModel(localSiteId)
                    CatalogItem.Variation(variationEntity)
                }
                else -> {
                    logger.w("Unknown catalog item type: $type")
                    CatalogItem.Unknown
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse catalog item of type $type: ${e.message}")
            CatalogItem.Unknown
        }
    }
}
