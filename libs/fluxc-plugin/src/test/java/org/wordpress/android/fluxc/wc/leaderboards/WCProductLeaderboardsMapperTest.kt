package org.wordpress.android.fluxc.wc.leaderboards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleProductList
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateStubbedProductIdList
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@RunWith(RobolectricTestRunner::class)
class WCProductLeaderboardsMapperTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(context)

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
        mapperUnderTest = WCProductLeaderboardsMapper()
        productStore = spy(
            WCProductStore(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                productsDao = databaseRule.db.productsDao,
                productVariationsDao = databaseRule.db.productVariationsDao,
                productCategoriesDao = databaseRule.db.productCategoriesDao,
                productTagsDao = databaseRule.db.productTagsDao,
                productShippingClassesDao = databaseRule.db.productShippingClassesDao,
                productReviewsDao = databaseRule.db.productReviewsDao
            )
        )
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
        whenever(productStore.fetchProductListSynced(stubSite, generateStubbedProductIdList)).thenReturn(expectedProducts)
    }

    private suspend fun configureFailingProductStoreMock() {
        whenever(productStore.fetchProductListSynced(stubSite, generateStubbedProductIdList)).thenReturn(null)
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
