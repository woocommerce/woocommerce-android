package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WPSettingsStore
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Locale

@ExperimentalCoroutinesApi
class CalendarHelperTest : BaseUnitTest() {
    private val originalLocale = Locale.getDefault()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn site
    }
    private val wpSettingsStore: WPSettingsStore = mock()

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `given sunday site start, when calendar is requested, then first day is sunday`() {
        givenStartOfWeek(DayOfWeek.SUNDAY)

        val calendar = createHelper().getCalendarForSelectedSite()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.SUNDAY)
    }

    @Test
    fun `given monday site start, when calendar is requested, then first day is monday`() {
        givenStartOfWeek(DayOfWeek.MONDAY)

        val calendar = createHelper().getCalendarForSelectedSite()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.MONDAY)
    }

    @Test
    fun `given saturday site start, when calendar is requested, then first day is saturday`() {
        givenStartOfWeek(DayOfWeek.SATURDAY)

        val calendar = createHelper().getCalendarForSelectedSite()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.SATURDAY)
    }

    @Test
    fun `given unavailable start, when calendar is requested, then default calendar is unchanged`() {
        Locale.setDefault(Locale.US)
        givenStartOfWeek(null)

        val calendar = createHelper().getCalendarForSelectedSite()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.getInstance().firstDayOfWeek)
    }

    @Test
    fun `given no selected site, when calendar is requested, then default calendar is unchanged`() {
        Locale.setDefault(Locale.US)
        val selectedSite: SelectedSite = mock {
            on { getOrNull() } doReturn null
        }

        val calendar = CalendarHelper(
            selectedSite = selectedSite,
            wpSettingsStore = wpSettingsStore
        ).getCalendarForSelectedSite()

        assertThat(calendar.firstDayOfWeek).isEqualTo(Calendar.getInstance().firstDayOfWeek)
    }

    private fun givenStartOfWeek(startOfWeek: DayOfWeek?) {
        doReturn(startOfWeek).`when`(wpSettingsStore).getStartOfWeek(site)
    }

    private fun createHelper() = CalendarHelper(
        selectedSite = selectedSite,
        wpSettingsStore = wpSettingsStore
    )

    private companion object {
        val site = SiteModel().apply {
            id = 1
        }
    }
}
