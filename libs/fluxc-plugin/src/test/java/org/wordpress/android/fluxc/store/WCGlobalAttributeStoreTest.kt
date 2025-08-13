package org.wordpress.android.fluxc.store

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.parsedAttributesList
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGlobalAttributeStoreTest {
    private lateinit var storeUnderTest: WCGlobalAttributeStore
    private lateinit var restClient: ProductAttributeRestClient
    private lateinit var mapper: WCGlobalAttributeMapper

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.getApplication().applicationContext
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(
                SiteModel::class.java,
                WCGlobalAttributeModel::class.java
            ),
            WellSqlConfig.Companion.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
        initMocks()
        createStoreUnderTest()
    }

    @Test
    fun `fetch attributes with empty result should return WooError`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(emptyArray()))
        val result = storeUnderTest.fetchStoreAttributes(stubSite)
        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch attributes should call mapper once`() = test {
        mapper = spy()
        createStoreUnderTest()
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(attributesFullListResponse))

        storeUnderTest.fetchStoreAttributes(stubSite)
        verify(mapper).responseToAttributeModelList(attributesFullListResponse!!, stubSite)
    }

    @Test
    fun `fetch attributes should return WooResult correctly`() = test {
        whenever(restClient.fetchProductFullAttributesList(stubSite))
            .thenReturn(WooPayload(attributesFullListResponse))

        whenever(mapper.responseToAttributeModelList(attributesFullListResponse!!, stubSite))
            .thenReturn(parsedAttributesList)

        storeUnderTest.fetchStoreAttributes(stubSite).let { result ->
            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(parsedAttributesList)
            assertThat(result.error).isNull()
        }
    }

    private fun initMocks() {
        restClient = mock()
        mapper = mock()
    }

    private fun createStoreUnderTest() =
        WCGlobalAttributeStore(
            restClient,
            mapper,
            initCoroutineEngine()
        ).apply { storeUnderTest = this }
}
