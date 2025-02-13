package org.wordpress.android.fluxc.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject

class ProductStorageHelper @Inject constructor(
    private val productSqlUtils: ProductSqlUtils,
    private val metaDataDao: MetaDataDao
) {
    suspend fun getProduct(site: SiteModel, remoteProductId: Long): ProductWithMetaData? {
        val product = withContext(Dispatchers.IO) {
            productSqlUtils.getProductByRemoteId(site, remoteProductId)
        } ?: return null
        val metadata = getProductMetadata(site, remoteProductId)
        return ProductWithMetaData(product, metadata)
    }

    suspend fun getProductMetadata(site: SiteModel, remoteProductId: Long): List<WCMetaData> {
        return metaDataDao.getMetaData(site.localId(), remoteProductId).map { it.toDomainModel() }
    }

    suspend fun upsertProduct(productWithMetaData: ProductWithMetaData): Int {
        val (product, metadata) = productWithMetaData
        val rowsAffected = withContext(Dispatchers.IO) {
            productSqlUtils.insertOrUpdateProduct(product)
        }

        metaDataDao.updateMetaData(
            parentItemId = product.remoteProductId,
            localSiteId = LocalId(product.localSiteId),
            metaData = metadata.map {
                MetaDataEntity.fromDomainModel(
                    metaData = it,
                    localSiteId = LocalId(product.localSiteId),
                    parentItemId = product.remoteProductId,
                    parentItemType = MetaDataParentItemType.PRODUCT
                )
            }
        )
        return rowsAffected
    }

    suspend fun upsertProducts(productsWithMetaData: List<ProductWithMetaData>): Int {
        val products = productsWithMetaData.map { it.product }
        val rowsAffected = withContext(Dispatchers.IO) {
            productSqlUtils.insertOrUpdateProducts(products)
        }

        productsWithMetaData.forEach { productWithMetaData ->
            val (product, metadata) = productWithMetaData
            metaDataDao.updateMetaData(
                parentItemId = product.remoteProductId,
                localSiteId = LocalId(product.localSiteId),
                metaData = metadata.map {
                    MetaDataEntity.fromDomainModel(
                        metaData = it,
                        localSiteId = LocalId(product.localSiteId),
                        parentItemId = product.remoteProductId,
                        parentItemType = MetaDataParentItemType.PRODUCT
                    )
                }
            )
        }

        return rowsAffected
    }

    suspend fun deleteProduct(site: SiteModel, remoteProductId: Long): Int {
        val rowsAffected = withContext(Dispatchers.IO) {
            productSqlUtils.deleteProduct(site, remoteProductId)
        }
        metaDataDao.deleteMetaData(site.localId(), remoteProductId)
        return rowsAffected
    }

    suspend fun deleteProductsForSite(site: SiteModel) {
        withContext(Dispatchers.IO) {
            productSqlUtils.deleteProductsForSite(site)
        }
        metaDataDao.deleteMetaDataForSite(site.localId(), MetaDataParentItemType.PRODUCT)
    }
}
