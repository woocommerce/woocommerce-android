package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.metadata.MetaDataRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.StripOrderMetaData
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaDataStore @Inject internal constructor(
    private val metaDataRestClient: MetaDataRestClient,
    private val metaDataDao: MetaDataDao,
    private val stripProductMetaData: StripProductMetaData,
    private val stripOrderMetaData: StripOrderMetaData
) {
    suspend fun updateMetaData(
        site: SiteModel,
        request: UpdateMetadataRequest
    ): WooResult<Unit> {
        val result = metaDataRestClient.updateMetaData(site, request)

        result.result?.let {
            persistMetaData(
                site = site,
                parentItemId = request.parentItemId,
                parentItemType = request.parentItemType,
                metaDataList = it
            )
        }

        @Suppress("RedundantUnitExpression")
        return result.asWooResult { Unit }
    }

    suspend fun refreshMetaData(
        site: SiteModel,
        parentItemId: Long,
        parentItemType: MetaDataParentItemType
    ): WooResult<Unit> {
        val result = metaDataRestClient.fetchMetaData(site, parentItemId, parentItemType)

        result.result?.let {
            persistMetaData(
                site = site,
                parentItemId = parentItemId,
                parentItemType = parentItemType,
                metaDataList = it
            )
        }

        @Suppress("RedundantUnitExpression")
        return result.asWooResult { Unit }
    }

    private suspend fun persistMetaData(
        site: SiteModel,
        parentItemId: Long,
        parentItemType: MetaDataParentItemType,
        metaDataList: List<WCMetaData>
    ) {
        val filteredValues = when (parentItemType) {
            MetaDataParentItemType.ORDER -> stripOrderMetaData(metaDataList)
            MetaDataParentItemType.PRODUCT -> stripProductMetaData(metaDataList)
        }

        metaDataDao.updateMetaData(
            localSiteId = site.localId(),
            parentItemId = parentItemId,
            metaData = filteredValues.map { metaData ->
                MetaDataEntity.fromDomainModel(
                    metaData = metaData,
                    localSiteId = site.localId(),
                    parentItemId = parentItemId,
                    parentItemType = parentItemType
                )
            }
        )
    }

    fun observeMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.observeMetaData(site.localId(), parentItemId)
        .map { list -> list.map { it.toDomainModel() } }

    fun observeDisplayableMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.observeDisplayableMetaData(site.localId(), parentItemId)
        .map { list -> list.map { it.toDomainModel() } }

    suspend fun getMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.getMetaData(site.localId(), parentItemId).map { it.toDomainModel() }

    suspend fun getDisplayableMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.getDisplayableMetaData(site.localId(), parentItemId).map { it.toDomainModel() }

    suspend fun getMetaDataById(
        site: SiteModel,
        parentItemId: Long,
        metaDataId: Long
    ) = metaDataDao.getMetaData(
        localSiteId = site.localId(),
        parentItemId = parentItemId,
        id = metaDataId
    )
        ?.toDomainModel()

    suspend fun getMetaDataByKey(
        site: SiteModel,
        parentItemId: Long,
        key: String
    ) = metaDataDao.getMetaDataByKey(site.localId(), parentItemId, key)?.map { it.toDomainModel() }

    suspend fun hasDisplayableMetaData(
        site: SiteModel,
        parentItemId: Long
    ) = metaDataDao.getDisplayableMetaDataCount(site.localId(), parentItemId) > 0
}
