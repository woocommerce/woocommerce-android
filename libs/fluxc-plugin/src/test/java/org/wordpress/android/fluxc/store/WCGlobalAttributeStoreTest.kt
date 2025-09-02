package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.dao.GlobalAttributesDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedAttributesList
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.stubSite

class WCGlobalAttributeStoreTest {
    private lateinit var storeUnderTest: WCGlobalAttributeStore
    private val restClient = mock<ProductAttributeRestClient>()
    private val globalAttributesDao = mock<GlobalAttributesDao>()
    private val mapper = WCGlobalAttributeMapper()

    @Before
    fun setUp() {
        storeUnderTest = WCGlobalAttributeStore(
            restClient,
            globalAttributesDao,
            mapper,
            initCoroutineEngine()
        )
    }

    @Test
    fun `fetch attributes with empty result should return WooError`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(emptyArray()))
        whenever(globalAttributesDao.getAttributesForSite(stubSite.localId()))
            .thenReturn(emptyList())

        val result = storeUnderTest.fetchStoreAttributes(stubSite)

        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch attributes should call mapper once`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(attributesFullListResponse))
        whenever(globalAttributesDao.getAttributesForSite(stubSite.localId()))
            .thenReturn(parsedAttributesList)

        val result = storeUnderTest.fetchStoreAttributes(stubSite)

        assertThat(result.model).isEqualTo(mapper.responseToAttributeModelList(attributesFullListResponse!!, stubSite))
    }

    @Test
    fun `fetch attributes should return WooResult correctly`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(attributesFullListResponse))
        whenever(globalAttributesDao.getAttributesForSite(stubSite.localId()))
            .thenReturn(parsedAttributesList)

        val result = storeUnderTest.fetchStoreAttributes(stubSite)

        assertThat(result.model).isNotNull
        assertThat(result.model).isEqualTo(parsedAttributesList)
        assertThat(result.error).isNull()
    }
}
