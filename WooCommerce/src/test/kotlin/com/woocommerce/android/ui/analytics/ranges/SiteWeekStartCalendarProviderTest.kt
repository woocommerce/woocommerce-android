package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WPSettingsStore
import java.util.Calendar
import java.util.Locale

@ExperimentalCoroutinesApi
class SiteWeekStartCalendarProviderTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn site
    }
    private val wpSettingsStore: WPSettingsStore = mock()

    @Test
    fun `given wp sunday start, when calendar is requested, then first day is sunday`() {
        givenStartOfWeek(SUNDAY)

        val calendar = createProvider().getCalendar()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.SUNDAY)
    }

    @Test
    fun `given wp monday start, when calendar is requested, then first day is monday`() {
        givenStartOfWeek(MONDAY)

        val calendar = createProvider().getCalendar()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.MONDAY)
    }

    @Test
    fun `given wp saturday start, when calendar is requested, then first day is saturday`() {
        givenStartOfWeek(SATURDAY)

        val calendar = createProvider().getCalendar()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.SATURDAY)
    }

    @Test
    fun `given unavailable start, when calendar is requested, then default calendar is unchanged`() {
        Locale.setDefault(Locale.US)
        givenStartOfWeek(null)

        val calendar = createProvider().getCalendar()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.getInstance().firstDayOfWeek)
    }

    @Test
    fun `given invalid cached start, when calendar is requested, then default calendar is unchanged`() {
        Locale.setDefault(Locale.US)
        givenStartOfWeek(INVALID)

        val calendar = createProvider().getCalendar()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.getInstance().firstDayOfWeek)
    }

    private fun givenStartOfWeek(startOfWeek: Int?) {
        doReturn(startOfWeek).`when`(wpSettingsStore).getStartOfWeek(site)
    }

    private fun createProvider() = SiteWeekStartCalendarProvider(
        selectedSite = selectedSite,
        wpSettingsStore = wpSettingsStore
    )

    private companion object {
        const val SUNDAY = 0
        const val MONDAY = 1
        const val SATURDAY = 6
        const val INVALID = 7

        val site = SiteModel().apply {
            id = 1
        }
    }
}
