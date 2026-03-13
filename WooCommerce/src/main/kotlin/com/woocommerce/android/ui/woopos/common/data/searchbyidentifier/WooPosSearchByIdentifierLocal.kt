package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.SyncStrategy
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosSearchByIdentifierLocal @Inject constructor(
    private val productsCache: WooPosProductsCache,
    private val variationsCache: WooPosVariationsLRUCache,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val productModelMapper: WooPosProductModelMapper,
    private val variationMapper: WooPosVariationMapper,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke(
        identifier: String,
        syncStrategy: SyncStrategy
    ): WooPosSearchByIdentifierResult {
        val siteId = selectedSite.get().localId()

        return when (syncStrategy) {
            SyncStrategy.LOCAL_CATALOG_FILE -> searchInLocalCatalog(identifier, siteId)
            SyncStrategy.REMOTE -> searchInMemoryCache(identifier)
        }
    }

    private suspend fun searchInLocalCatalog(
        identifier: String,
        siteId: LocalOrRemoteId.LocalId
    ): WooPosSearchByIdentifierResult {
        localCatalogStore.findEvenUnsupportedProductByIdentifier(siteId, identifier).getOrNull()
            ?.let { productEntity ->
                val productModel = productModelMapper.fromEntity(productEntity)
                return WooPosSearchByIdentifierResult.Success(productModel)
            }

        localCatalogStore.findEvenUnsupportedVariationByIdentifier(siteId, identifier)
            .getOrNull()?.let { variationEntity ->
                val variation = variationMapper.fromWooPosVariationEntity(variationEntity)

                val parentProductResult = localCatalogStore.getProduct(
                    siteId,
                    LocalOrRemoteId.RemoteId(variationEntity.remoteProductId.value)
                )

                return parentProductResult.getOrNull()?.let { parentEntity ->
                    val parentProduct = productModelMapper.fromEntity(parentEntity)
                    WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct)
                } ?: WooPosSearchByIdentifierResult.Failure(
                    WooPosSearchByIdentifierResult.Error.NotFound
                )
            }

        return WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NotFound
        )
    }

    private suspend fun searchInMemoryCache(identifier: String): WooPosSearchByIdentifierResult {
        findProductByIdentifier(identifier)?.let {
            return WooPosSearchByIdentifierResult.Success(it)
        }

        return findVariationWithParentByIdentifier(identifier)
    }

    private suspend fun findProductByIdentifier(identifier: String) =
        productsCache.getAll().firstOrNull { product ->
            product.globalUniqueId.equals(identifier, ignoreCase = true)
        }

    private suspend fun findVariationWithParentByIdentifier(identifier: String): WooPosSearchByIdentifierResult {
        val variation = variationsCache.getAll().firstOrNull { variation ->
            variation.globalUniqueId.equals(identifier, ignoreCase = true)
        } ?: return WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NotFound
        )

        return productsCache.getProductById(variation.remoteProductId)?.let { parentProduct ->
            WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct)
        } ?: WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NotFound
        )
    }
}
