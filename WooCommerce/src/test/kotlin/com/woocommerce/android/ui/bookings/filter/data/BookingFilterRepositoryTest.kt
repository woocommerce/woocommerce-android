package com.woocommerce.android.ui.bookings.filter.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingFilterRepositoryTest : BaseUnitTest() {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempFile: Path

    private val siteState = MutableStateFlow<SiteModel?>(null)
    private var currentSiteId: Int = -1

    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn siteState
    }

    @Before
    fun setUp() {
        tempFile = Files.createTempFile("bfilters_test", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.toString().toPath() }
        )
    }

    @After
    fun tearDown() {
        try {
            Files.deleteIfExists(tempFile)
        } catch (_: Throwable) {
        }
    }

    private fun setSite(id: Int): SiteModel {
        currentSiteId = id
        val site: SiteModel = mock()
        whenever(site.id).thenReturn(id)
        whenever(selectedSite.getSelectedSiteId()).thenReturn(currentSiteId)
        siteState.value = site
        return site
    }

    @Test
    fun `when no values are saved, then bookingFilterFlow emits empty filter`() = testBlocking {
        // GIVEN
        setSite(1)
        val repository = BookingFilterRepository(dataStore, selectedSite)

        // WHEN
        val value = repository.bookingFiltersFlow.first()

        // THEN
        assertThat(value).isEqualTo(BookingFilters())
        assertThat(value.customer).isNull()
        assertThat(value.dateRange).isNull()
    }

    @Test
    fun `when customer and dateRange saved, then flow emits them`() = testBlocking {
        // GIVEN
        setSite(42)
        val repository = BookingFilterRepository(dataStore, selectedSite)
        val before = Instant.parse("2025-01-02T00:00:00Z")
        val after = Instant.parse("2025-01-01T00:00:00Z")
        val customer = BookingsFilterOption.Customer(customerId = 7L, customerName = "Alice")
        val toSave = BookingFilters(
            customer = customer,
            dateRange = BookingsFilterOption.DateRange(before = before, after = after)
        )

        // WHEN
        repository.save(toSave)
        val emitted = repository.bookingFiltersFlow.first()

        // THEN
        assertThat(emitted.customer).isEqualTo(customer)
        assertThat(emitted.dateRange?.before).isEqualTo(before)
        assertThat(emitted.dateRange?.after).isEqualTo(after)
    }

    @Test
    fun `when site changes, then emitted values are isolated per site`() = testBlocking {
        // GIVEN site A with saved values
        val repository = BookingFilterRepository(dataStore, selectedSite)
        setSite(100)
        val siteACustomer = BookingsFilterOption.Customer(1L, "Customer A")
        repository.save(BookingFilters(customer = siteACustomer))
        // Ensure emission with site A
        val firstA = repository.bookingFiltersFlow.first()
        assertThat(firstA.customer).isEqualTo(siteACustomer)

        // WHEN switch to site B (no values yet)
        setSite(200)
        val firstBEmpty = repository.bookingFiltersFlow.first()
        // THEN empty for site B
        assertThat(firstBEmpty).isEqualTo(BookingFilters())

        // WHEN save for site B and read again
        val siteBCustomer = BookingsFilterOption.Customer(2L, "Customer B")
        repository.save(BookingFilters(customer = siteBCustomer))
        val secondB = repository.bookingFiltersFlow.first()

        // THEN
        assertThat(secondB.customer).isEqualTo(siteBCustomer)

        // AND switch back to site A still has A's values
        setSite(100)
        val secondA = repository.bookingFiltersFlow.first()
        assertThat(secondA.customer).isEqualTo(siteACustomer)
    }
}
