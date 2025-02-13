package org.wordpress.android.fluxc.store

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.metadata.MetaDataRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.StripOrderMetaData
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity

@RunWith(Parameterized::class)
class MetaDataStoreTest {
    private lateinit var metaDataStore: MetaDataStore
    private val metaDataRestClient: MetaDataRestClient = mock()
    private val metaDataDao: MetaDataDao = mock()
    private val stripProductMetaData = StripProductMetaData()
    private val stripOrderMetaData = StripOrderMetaData()

    @Parameter
    lateinit var parentItemType: MetaDataParentItemType

    private val site = SiteModel()

    @Before
    fun setUp() {
        metaDataStore = MetaDataStore(
            metaDataRestClient = metaDataRestClient,
            metaDataDao = metaDataDao,
            stripProductMetaData = stripProductMetaData,
            stripOrderMetaData = stripOrderMetaData
        )
    }

    @Test
    fun `when updating order metadata successfully, then persist metadata`() = runTest {
        val request = UpdateMetadataRequest(
            parentItemId = 1,
            parentItemType = parentItemType,
            insertedMetadata = listOf(WCMetaData(0, "key", "value"))
        )
        val payload = WooPayload(
            listOf(
                WCMetaData(1, "key", "value"),
                WCMetaData(2, "_key", "value2") // should be stripped
            )
        )
        whenever(metaDataRestClient.updateMetaData(site, request)).thenReturn(payload)

        val result = metaDataStore.updateMetaData(site, request)

        assertThat(result).isEqualTo(WooResult(Unit))
        verify(metaDataDao).updateMetaData(
            parentItemId = 1,
            localSiteId = site.localId(),
            metaData = parentItemType.stripper(payload.result!!).map {
                MetaDataEntity.fromDomainModel(it, site.localId(), 1, parentItemType)
            }
        )
    }

    @Test
    fun `when refreshing metadata succeeds, then persist metadata`() = runTest {
        val payload = WooPayload(
            listOf(
                WCMetaData(1, "key", "value"),
                WCMetaData(2, "_key", "value2") // should be stripped
            )
        )
        whenever(metaDataRestClient.fetchMetaData(site, 1, parentItemType))
            .thenReturn(payload)

        val result = metaDataStore.refreshMetaData(site, 1, parentItemType)

        assertThat(result).isEqualTo(WooResult(Unit))
        verify(metaDataDao).updateMetaData(
            parentItemId = 1,
            localSiteId = site.localId(),
            metaData = parentItemType.stripper(payload.result!!).map {
                MetaDataEntity.fromDomainModel(it, site.localId(), 1, parentItemType)
            }
        )
    }

    @Test
    fun `when updating metadata fails, then do not persist metadata`() = runTest {
        val request = UpdateMetadataRequest(
            parentItemId = 1,
            parentItemType = parentItemType,
            insertedMetadata = listOf(WCMetaData(0, "key", "value"))
        )
        val error = WooError(type = WooErrorType.GENERIC_ERROR, original = GenericErrorType.UNKNOWN)
        whenever(metaDataRestClient.updateMetaData(site, request)).thenReturn(WooPayload(error))

        val result = metaDataStore.updateMetaData(site, request)

        assertThat(result).isEqualTo(WooResult<Unit>(error))
        verify(metaDataDao, never()).updateMetaData(
            parentItemId = 1,
            localSiteId = site.localId(),
            metaData = emptyList()
        )
    }

    @Test
    fun `when refreshing metadata fails, then do not persist metadata`() = runTest {
        val error = WooError(type = WooErrorType.GENERIC_ERROR, original = GenericErrorType.UNKNOWN)
        whenever(metaDataRestClient.fetchMetaData(site, 1, parentItemType))
            .thenReturn(WooPayload(error))

        val result = metaDataStore.refreshMetaData(site, 1, parentItemType)

        assertThat(result).isEqualTo(WooResult<Unit>(error))
        verify(metaDataDao, never()).updateMetaData(
            parentItemId = 1,
            localSiteId = site.localId(),
            metaData = emptyList()
        )
    }

    private val MetaDataParentItemType.stripper
        get() = when (this) {
            MetaDataParentItemType.ORDER -> stripOrderMetaData::invoke
            MetaDataParentItemType.PRODUCT -> stripProductMetaData::invoke
        }

    companion object {
        @JvmStatic
        @Parameters
        fun data() = MetaDataParentItemType.entries
    }
}
