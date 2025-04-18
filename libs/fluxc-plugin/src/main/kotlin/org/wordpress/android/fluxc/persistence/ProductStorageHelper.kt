package org.wordpress.android.fluxc.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject

class ProductStorageHelper @Inject constructor(
    private val productsDao: ProductsDao,
    private val metaDataDao: MetaDataDao
) {
    suspend fun getProduct(site: SiteModel, remoteProductId: Long): ProductWithMetaData? {
        val product = productsDao.getProduct(site.id, remoteProductId) ?: return null
        val metadata = getProductMetadata(site, remoteProductId)
        return ProductWithMetaData(product, metadata)
    }

    suspend fun getProductMetadata(site: SiteModel, remoteProductId: Long): List<WCMetaData> {
        return metaDataDao.getMetaData(site.localId(), remoteProductId).map { it.toDomainModel() }
    }

    suspend fun upsertProduct(productWithMetaData: ProductWithMetaData): Int {
        val (product, metadata) = productWithMetaData
        productsDao.upsertProduct(product)

        metaDataDao.updateMetaData(
            parentItemId = product.remoteProductId,
            localSiteId = product.localSiteId,
            metaData = metadata.map {
                MetaDataEntity.fromDomainModel(
                    metaData = it,
                    localSiteId = product.localSiteId,
                    parentItemId = product.remoteProductId,
                    parentItemType = MetaDataParentItemType.PRODUCT
                )
            }
        )
        return 0 //TODO: check the consequences: Room doesn't offer "rowsAffected" value for @Upsert
    }

    suspend fun upsertProducts(productsWithMetaData: List<ProductWithMetaData>): Int {
        val products = productsWithMetaData.map { it.product }

        productsDao.upsertProducts(products)

        productsWithMetaData.forEach { productWithMetaData ->
            val (product, metadata) = productWithMetaData
            metaDataDao.updateMetaData(
                parentItemId = product.remoteProductId,
                localSiteId = product.localSiteId,
                metaData = metadata.map {
                    MetaDataEntity.fromDomainModel(
                        metaData = it,
                        localSiteId = product.localSiteId,
                        parentItemId = product.remoteProductId,
                        parentItemType = MetaDataParentItemType.PRODUCT
                    )
                }
            )
        }
        return 0 //TODO: check the consequences: Room doesn't offer "rowsAffected" value for @Upsert
    }

    suspend fun deleteProduct(site: SiteModel, remoteProductId: Long): Int {
        val rowsAffected = withContext(Dispatchers.IO) {
            productsDao.deleteProduct(site.id, remoteProductId)
        }
        metaDataDao.deleteMetaData(site.localId(), remoteProductId)
        return rowsAffected
    }

    suspend fun deleteProductsForSite(site: SiteModel) {
        productsDao.deleteProducts(site.id)
        metaDataDao.deleteMetaDataForSite(site.localId(), MetaDataParentItemType.PRODUCT)
    }
}
