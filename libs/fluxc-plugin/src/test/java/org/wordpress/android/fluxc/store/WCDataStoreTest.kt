package org.wordpress.android.fluxc.store

import androidx.room.Room
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
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.LocationsDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.CountryTestUtils

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCDataStoreTest {
    private val restClient = mock<WCDataRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private lateinit var roomDb: WCAndroidDatabase
    private lateinit var locationsDao: LocationsDao
    private lateinit var store: WCDataStore

    private val sampleData = CountryTestUtils.generateCountries().sortedBy { it.code }
    private val sampleResponse = CountryTestUtils.generateCountryApiResponse()

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
            appContext, listOf(SiteModel::class.java), WellSqlConfig.Companion.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        roomDb = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        locationsDao = roomDb.locationsDao

        store = WCDataStore(restClient, initCoroutineEngine(), locationsDao)

        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `given fetched countries, requesting countries returns a list of all countries`() = test {
        fetchCountries()
        val sampleCountries = sampleData.filter { it.parentCode == "" }

        val countries = store.getCountries().sortedBy { it.code }

        assertThat(countries).isEqualTo(sampleCountries)
    }

    @Test
    fun `given fetched countries, requesting states for a country with states returns the expected list of states`() =
        test {
            fetchCountries()
            val sampleStates = sampleData.filter { it.parentCode == "CA" }.sortedBy { it.code }

            val states = store.getStates("CA").sortedBy { it.code }

            assertThat(states).isEqualTo(sampleStates)
        }

    @Test
    fun `given fetched countries, requesting states for a country without states returns empty list`() =
        test {
            fetchCountries()

            val states = store.getStates("CZ")

            assertThat(states).isEqualTo(emptyList<WCLocationModel>())
        }

    @Test
    fun `given fetched countries, requesting states with empty country code returns empty list`() =
        test {
            fetchCountries()

            val states = store.getStates("")

            assertThat(states).isEqualTo(emptyList<WCLocationModel>())
        }

    private suspend fun fetchCountries(): WooResult<List<WCLocationModel>> {
        val payload = WooPayload(sampleResponse.toTypedArray())
        whenever(restClient.fetchCountries(site)).thenReturn(payload)
        return store.fetchCountriesAndStates(site)
    }
}
