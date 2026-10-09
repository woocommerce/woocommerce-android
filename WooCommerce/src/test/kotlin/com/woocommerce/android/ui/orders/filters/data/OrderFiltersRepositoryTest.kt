package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class OrderFiltersRepositoryTest {
    private lateinit var defaultTimeZone: TimeZone

    private var storedDays = Pair(0L, 0L)

    private val appSharedPrefs: AppPrefsWrapper = mock {
        on { setOrderFilterCustomDateRangeDays(any(), any(), any()) } doAnswer {
            storedDays = Pair(it.getArgument(1), it.getArgument(2))
        }
        on { getOrderFilterCustomDateRangeDays(any()) } doAnswer { storedDays }
        on { getOrderFilters(any(), any()) } doReturn ""
        on { getOrderFilters(any(), eq(OrderListFilterCategory.DATE_RANGE.name)) } doReturn
            DateRange.CUSTOM_RANGE.filterKey
    }
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doReturn SiteModel().apply { id = 1 }
        on { observe() } doReturn emptyFlow()
    }

    private val sut = OrderFiltersRepository(
        appSharedPrefs = appSharedPrefs,
        customerStore = mock(),
        selectedSite = selectedSite,
        appCoroutineScope = TestScope()
    )

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `given a saved range, when the device timezone changes, then the same days are returned`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        sut.setCustomDateRange(
            startDay = LocalDate.of(2026, 8, 1).toEpochDay(),
            endDay = LocalDate.of(2026, 8, 5).toEpochDay()
        )

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val range = sut.getCustomDateRangeFilter()

        assertThat(range.first.toLocalDate("America/Los_Angeles")).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(range.second.toLocalDate("America/Los_Angeles")).isEqualTo(LocalDate.of(2026, 8, 5))
    }

    @Test
    fun `given custom range is selected without a saved range, when reading the date selection, then it is empty`() {
        val selection = sut.getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE)

        assertThat(selection).isEmpty()
    }

    @Test
    fun `given custom range is selected with a saved range, when reading the date selection, then it is returned`() {
        sut.setCustomDateRange(
            startDay = LocalDate.of(2026, 8, 1).toEpochDay(),
            endDay = LocalDate.of(2026, 8, 5).toEpochDay()
        )

        val selection = sut.getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE)

        assertThat(selection).containsExactly(DateRange.CUSTOM_RANGE.filterKey)
    }

    private fun Long.toLocalDate(zoneId: String): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.of(zoneId)).toLocalDate()
}
