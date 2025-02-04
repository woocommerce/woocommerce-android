package org.wordpress.android.fluxc.wc.data

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
import org.wordpress.android.fluxc.model.data.WCCountryMapper
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCDataStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCDataStoreTest {
    private val restClient = mock<WCDataRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private val mapper = WCCountryMapper()
    private lateinit var store: WCDataStore

    private val sampleData = CountryTestUtils.generateCountries().sortedBy { it.code }
    private val sampleResponse = CountryTestUtils.generateCountryApiResponse()

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCLocationModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCDataStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )

        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `fetch countries`() = test {
        val result = fetchCountries()

        assertThat(result.model?.size).isEqualTo(sampleData.size)
        val first = mapper.map(sampleResponse.first()).first()
        assertThat(result.model?.first()?.name).isEqualTo(first.name)
        assertThat(result.model?.first()?.code).isEqualTo(first.code)
        assertThat(result.model?.first()?.parentCode).isEqualTo(first.parentCode)
    }

    @Test
    fun `get countries`() = test {
        fetchCountries()

        val sampleCountries = sampleData.filter { it.parentCode == "" }
        val countries = store.getCountries().sortedBy { it.code }

        assertThat(countries.size).isEqualTo(sampleCountries.size)

        countries.forEachIndexed { i, country ->
            assertThat(country.code).isEqualTo(sampleCountries[i].code)
            assertThat(country.name).isEqualTo(sampleCountries[i].name)
            assertThat(country.parentCode).isEqualTo(sampleCountries[i].parentCode)
        }
    }

    @Test
    fun `get non-empty states`() = test {
        fetchCountries()

        val sampleStates = sampleData.filter { it.parentCode == "CA" }.sortedBy { it.code }
        val states = store.getStates("CA").sortedBy { it.code }

        assertThat(states.size).isEqualTo(sampleStates.size)

        states.forEachIndexed { i, state ->
            assertThat(state.code).isEqualTo(sampleStates[i].code)
            assertThat(state.name).isEqualTo(sampleStates[i].name)
            assertThat(state.parentCode).isEqualTo(sampleStates[i].parentCode)
        }
    }

    @Test
    fun `get empty states`() = test {
        fetchCountries()

        val states = store.getStates("CZ")

        assertThat(states).isEqualTo(emptyList<WCLocationModel>())
    }

    @Test
    fun `when empty country code is passed, then empty list is returned when getting states`() = test {
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
