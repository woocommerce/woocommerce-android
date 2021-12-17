package com.woocommerce.android.ui.analytics

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnalyticsStorageTest : BaseUnitTest() {
    private val sut = AnalyticsStorage()

    @Test
    fun `given an empty storage, when getStats first time, then result is null`() {
        val result = sut.getStats(START_DATE, END_DATE)

        assertNull(result)
    }

    @Test
    fun `given a data in storage, when getStats first time, then result is expected`() {
        val stats: WCRevenueStatsModel = mock()
        sut.saveStats(START_DATE, END_DATE, stats)

        val result = sut.getStats(START_DATE, END_DATE)

        assertEquals(stats, result)
    }

    @Test
    fun `given a data in storage with 3 hits and save stats, when get stats, then result is expected`() {
        val stats: WCRevenueStatsModel = mock()
        sut.saveStats(START_DATE, END_DATE, stats)
        sut.getStats(START_DATE, END_DATE)
        sut.getStats(START_DATE, END_DATE)
        sut.getStats(START_DATE, END_DATE)

        sut.saveStats(START_DATE, END_DATE, stats)
        val result = sut.getStats(START_DATE, END_DATE)

        assertEquals(stats, result)
    }

    @Test
    fun `given a data in storage with 3 hits, when save stats, then result is null`() {
        val stats: WCRevenueStatsModel = mock()
        sut.saveStats(START_DATE, END_DATE, stats)
        sut.getStats(START_DATE, END_DATE)
        sut.getStats(START_DATE, END_DATE)
        sut.getStats(START_DATE, END_DATE)

        val result = sut.getStats(START_DATE, END_DATE)

        assertNull(result)
    }

    companion object {
        const val START_DATE = "10-10-2020"
        const val END_DATE = "12-10-2020"
    }
}
