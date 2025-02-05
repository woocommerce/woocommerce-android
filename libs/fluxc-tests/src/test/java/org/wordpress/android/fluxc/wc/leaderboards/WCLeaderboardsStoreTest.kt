package org.wordpress.android.fluxc.wc.leaderboards

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleTopPerformerApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCLeaderboardsStoreTest {
    private val leaderboardsRestClient: LeaderboardsRestClient = mock()
    private val reportsRestClient: ReportsRestClient = mock()
    private val productStore: WCProductStore = mock()
    private var mapper: WCProductLeaderboardsMapper = spy { WCProductLeaderboardsMapper() }
    private val topPerformersDao: TopPerformerProductsDao = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private lateinit var storeUnderTest: WCLeaderboardsStore

    fun setup(prepareMocks: () -> Unit = {}) {
        prepareMocks()
        createStoreUnderTest()
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(
                SiteModel::class.java,
                WCTopPerformerProductModel::class.java,
                WCProductModel::class.java
            ),
            WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `fetch top performer products legacy with empty result should return WooError`() = test {
        givenFetchLeaderBoardsReturns(emptyArray())
        setup()

        val result = storeUnderTest.fetchTopPerformerProductsLegacy(
            site = stubSite,
            startDate = START_DATE,
            endDate = END_DATE
        )

        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch top performer products legacy should filter leaderboards by PRODUCTS type`() = test {
        setup { mapper = spy() }
        val response = generateSampleLeaderboardsApiResponse()
        givenFetchLeaderBoardsReturns(response)

        storeUnderTest.fetchTopPerformerProductsLegacy(
            site = stubSite,
            startDate = START_DATE,
            endDate = END_DATE
        )

        verify(mapper).mapTopPerformerProductsEntity(
            response?.firstOrNull { it.type == PRODUCTS }!!,
            stubSite,
            productStore,
            DATE_PERIOD
        )
    }

    @Test
    fun `fetch top performer products legacy should call mapper once`() = test {
        setup()
        val response = generateSampleLeaderboardsApiResponse()
        givenFetchLeaderBoardsReturns(response)

        storeUnderTest.fetchTopPerformerProductsLegacy(
            site = stubSite,
            startDate = START_DATE,
            endDate = END_DATE
        )

        verify(mapper, times(1)).mapTopPerformerProductsEntity(
            any<LeaderboardsApiResponse>(),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `fetch top performer products legacy should return mapped top performer entities correctly`() =
        test {
            setup()
            val response = generateSampleLeaderboardsApiResponse()
            givenFetchLeaderBoardsReturns(response)
            givenTopPerformersMapperReturns(
                givenResponse = response?.firstOrNull { it.type == PRODUCTS }!!,
                returnedTopPerformersList = TOP_PERFORMER_ENTITY_LIST
            )

            val result = storeUnderTest.fetchTopPerformerProductsLegacy(
                site = stubSite,
                startDate = START_DATE,
                endDate = END_DATE
            )

            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(TOP_PERFORMER_ENTITY_LIST)
            assertThat(result.error).isNull()
        }

    @Test
    fun `fetch top performer products should return mapped top performer entities correctly`() =
        test {
            setup()
            val response = generateSampleTopPerformerApiResponse()
            givenFetchTopPerformerReturns(response)

            val topPerformers = storeUnderTest.fetchTopPerformerProducts(
                site = stubSite,
                startDate = START_DATE,
                endDate = END_DATE
            )
            val result = setMillisSinceLastUpdated(topPerformers, DEFAULT_MILLIS_SINCE_LAST_UPDATE)

            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(TOP_PERFORMER_ENTITY_REPORTS_LIST)
            assertThat(result.error).isNull()
        }

    @Test
    fun `fetching top performer products should update database with new data`() =
        test {
            setup()
            val response = generateSampleTopPerformerApiResponse()
            givenFetchTopPerformerReturns(response)

            storeUnderTest.fetchTopPerformerProducts(
                site = stubSite,
                startDate = START_DATE,
                endDate = END_DATE
            )

            verify(topPerformersDao).updateTopPerformerProductsFor(
                eq(stubSite.localId()),
                eq(DATE_PERIOD),
                any()
            )
        }

    @Test
    fun `fetching top performer products legacy should update database with new data`() =
        test {
            setup()
            val response = generateSampleLeaderboardsApiResponse()
            givenFetchLeaderBoardsReturns(response)
            givenTopPerformersMapperReturns(
                givenResponse = response?.firstOrNull { it.type == PRODUCTS }!!,
                returnedTopPerformersList = TOP_PERFORMER_ENTITY_LIST
            )

            storeUnderTest.fetchTopPerformerProductsLegacy(
                site = stubSite,
                startDate = START_DATE,
                endDate = END_DATE
            )

            verify(topPerformersDao, times(1))
                .updateTopPerformerProductsFor(
                    stubSite.localId(),
                    DATE_PERIOD,
                    TOP_PERFORMER_ENTITY_LIST
                )
        }

    @Test
    fun `invalidating top performer products should update database`() =
        test {
            setup()
            givenCachedTopPerformers()

            storeUnderTest.invalidateTopPerformers(stubSite)

            verify(topPerformersDao, times(1))
                .getTopPerformerProductsForSite(stubSite.localId())
            verify(topPerformersDao, times(1))
                .updateTopPerformerProductsForSite(
                    stubSite.localId(),
                    INVALIDATED_TOP_PERFORMER_ENTITY_LIST
                )
        }

    private suspend fun givenCachedTopPerformers() {
        whenever(
            topPerformersDao.getTopPerformerProductsForSite(stubSite.localId())
        ).thenReturn(TOP_PERFORMER_ENTITY_LIST)
    }

    private suspend fun givenFetchLeaderBoardsReturns(response: Array<LeaderboardsApiResponse>?) {
        whenever(
            leaderboardsRestClient.fetchLeaderboards(
                site = any(),
                startDate = any(),
                endDate = any(),
                quantity = anyOrNull(),
                forceRefresh = any(),
                interval = any(),
                addProductsPath = any(),
            )
        ).thenReturn(WooPayload(response))
    }

    private suspend fun givenFetchTopPerformerReturns(response: Array<ReportsProductApiResponse>?) {
        whenever(
            reportsRestClient.fetchTopPerformerProducts(
                site = any(),
                startDate = any(),
                endDate = any(),
                quantity = any()
            )
        ).thenReturn(WooPayload(response))
    }

    private fun createStoreUnderTest() {
        storeUnderTest = WCLeaderboardsStore(
            leaderboardsRestClient,
            productStore,
            mapper,
            initCoroutineEngine(),
            topPerformersDao,
            reportsRestClient,
            wooCommerceStore
        )
    }

    private suspend fun givenTopPerformersMapperReturns(
        givenResponse: LeaderboardsApiResponse,
        returnedTopPerformersList: List<TopPerformerProductEntity>,
        siteModel: SiteModel = stubSite
    ) {
        whenever(
            mapper.mapTopPerformerProductsEntity(
                givenResponse,
                siteModel,
                productStore,
                DATE_PERIOD
            )
        ).thenReturn(returnedTopPerformersList)
    }

    private fun setMillisSinceLastUpdated(
        result: WooResult<List<TopPerformerProductEntity>>,
        millis: Long
    ): WooResult<List<TopPerformerProductEntity>> {
        val updatedList = result.model?.map { entity ->
            entity.copy(millisSinceLastUpdated = millis)
        }
        return WooResult(updatedList)
    }

    companion object {
        const val DEFAULT_MILLIS_SINCE_LAST_UPDATE = 100L
        const val START_DATE = "2024-01-01"
        const val END_DATE = "2024-01-31"
        const val DATE_PERIOD = "$START_DATE-$END_DATE"
        val TOP_PERFORMER_ENTITY_LIST =
            listOf(
                TopPerformerProductEntity(
                    localSiteId = LocalId(1),
                    datePeriod = DATE_PERIOD,
                    productId = RemoteId(123),
                    name = "product",
                    imageUrl = null,
                    quantity = 5,
                    currency = "USD",
                    total = 10.5,
                    millisSinceLastUpdated = 100
                )
            )
        val INVALIDATED_TOP_PERFORMER_ENTITY_LIST =
            listOf(
                TopPerformerProductEntity(
                    localSiteId = LocalId(1),
                    datePeriod = DATE_PERIOD,
                    productId = RemoteId(123),
                    name = "product",
                    imageUrl = null,
                    quantity = 5,
                    currency = "USD",
                    total = 10.5,
                    millisSinceLastUpdated = 0
                )
            )

        @Suppress("MaxLineLength")
        val TOP_PERFORMER_ENTITY_REPORTS_LIST =
            listOf(
                TopPerformerProductEntity(
                    localSiteId = LocalId(321),
                    datePeriod = DATE_PERIOD,
                    productId = RemoteId(14),
                    name = "Polo",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/polo-2-600x599.jpg",
                    quantity = 25,
                    currency = "",
                    total = 506.0000000000001,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                ),
                TopPerformerProductEntity(
                    localSiteId = LocalId(321),
                    datePeriod = DATE_PERIOD,
                    productId = RemoteId(22),
                    name = "Sunglasses Subscription",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/sunglasses-2-600x600.jpg",
                    quantity = 25,
                    currency = "",
                    total = 1374.0,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                ),
                TopPerformerProductEntity(
                    localSiteId = LocalId(321),
                    datePeriod = DATE_PERIOD,
                    productId = RemoteId(15),
                    name = "Beanie",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/beanie-2-600x600.jpg",
                    quantity = 21,
                    currency = "",
                    total = 385.0,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                )
            )
    }
}
