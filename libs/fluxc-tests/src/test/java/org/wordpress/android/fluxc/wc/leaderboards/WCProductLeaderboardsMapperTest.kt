package org.wordpress.android.fluxc.wc.leaderboards

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleProductList
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateStubbedProductIdList
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductLeaderboardsMapperTest {
    companion object {
        const val DATE_PERIOD = "2024-01-01-2024-01-31"
    }

    private lateinit var mapperUnderTest: WCProductLeaderboardsMapper
    private lateinit var productStore: WCProductStore

    private val expectedProducts = generateSampleProductList()
    private val productApiResponse = generateSampleLeaderboardsApiResponse()
            ?.firstOrNull { it.type == PRODUCTS }

    @Before
    fun setUp() {
        SingleStoreWellSqlConfigForTests(
                RuntimeEnvironment.application.applicationContext,
                listOf(SiteModel::class.java, WCProductModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        ).let {
            WellSql.init(it)
            it.reset()
        }

        mapperUnderTest = WCProductLeaderboardsMapper()
        productStore = mock()
    }

    @Test
    fun `map should request and parse all products from id`() = test {
        configureProductStoreMock()
        val result = mapperUnderTest.mapTopPerformerProductsEntity(
                productApiResponse!!,
                stubSite,
                productStore,
                DATE_PERIOD
        )
        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(3)
    }

    @Test
    fun `map should request and parse all products from id removing failing ones`() = test {
        configureProductStoreMock()
        configureExactFailingProductStoreMock(15)
        val result = mapperUnderTest.mapTopPerformerProductsEntity(
                productApiResponse!!,
                stubSite,
                productStore,
                DATE_PERIOD
        )
        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `map should remove any failing product response from the result`() = test {
        configureFailingProductStoreMock()
        val result = mapperUnderTest.mapTopPerformerProductsEntity(
                productApiResponse!!,
                stubSite,
                productStore,
                DATE_PERIOD
        )
        assertThat(result).isNotNull
        assertThat(result).isEmpty()
    }

    private suspend fun configureProductStoreMock() {
        generateStubbedProductIdList
                .let {
                    whenever(productStore.fetchProductListSynced(stubSite, it))
                            .thenReturn(expectedProducts)
                }
    }

    private suspend fun configureFailingProductStoreMock() {
        generateStubbedProductIdList
                .let {
                    whenever(productStore.fetchProductListSynced(stubSite, it))
                            .thenReturn(null)
                }
    }

    private suspend fun configureExactFailingProductStoreMock(productId: Long) {
        expectedProducts
                .filter { it.remoteProductId != productId }
                .let {
                    whenever(
                            productStore
                                    .fetchProductListSynced(
                                            stubSite,
                                            generateStubbedProductIdList
                                    )
                    ).thenReturn(it)
                }
    }
}
