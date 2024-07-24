package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.stats.GetSelectedRangeForDashboardStats
import com.woocommerce.android.ui.dashboard.topperformers.GetSelectedRangeForTopPerformers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateAnalyticsDashboardRangeSelectionsTest : BaseUnitTest() {
    private val getSelectedRangeForTopPerformers: GetSelectedRangeForTopPerformers = mock()
    private val getSelectedRangeForDashboardStats: GetSelectedRangeForDashboardStats = mock()
    private val updateAnalyticsData: UpdateAnalyticsDataByRangeSelection = mock()

    private val today = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault(),
        referenceStartDate = Date(1719858929),
        referenceEndDate = Date(1719858929)
    )

    private val yesterday = StatsTimeRangeSelection.SelectionType.YESTERDAY.generateSelectionData(
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault(),
        referenceStartDate = Date(1719794129),
        referenceEndDate = Date(1719794129)
    )

    private val sut = UpdateAnalyticsDashboardRangeSelections(
        getSelectedRangeForTopPerformers = getSelectedRangeForTopPerformers,
        getSelectedRangeForDashboardStats = getSelectedRangeForDashboardStats,
        updateAnalyticsData = updateAnalyticsData
    )

    @Test
    fun `when there are two different range selected then update data for both ranges`() = runTest {
        val forceCardUpdates = listOf(AnalyticsCards.Products, AnalyticsCards.Revenue, AnalyticsCards.Session)

        whenever(getSelectedRangeForTopPerformers.invoke()).doReturn(flowOf(today))
        whenever(getSelectedRangeForDashboardStats.invoke()).doReturn(flowOf(yesterday))
        whenever(
            updateAnalyticsData.invoke(
                selectedRange = yesterday,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)
        whenever(
            updateAnalyticsData.invoke(
                selectedRange = today,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)

        sut.invoke()

        verify(updateAnalyticsData).invoke(
            selectedRange = today,
            forceCardUpdates = forceCardUpdates
        )
        verify(updateAnalyticsData).invoke(
            selectedRange = yesterday,
            forceCardUpdates = forceCardUpdates
        )
    }

    @Test
    fun `when the same range is selected then update for the selected ranges once`() = runTest {
        val forceCardUpdates = listOf(AnalyticsCards.Products, AnalyticsCards.Revenue, AnalyticsCards.Session)

        whenever(getSelectedRangeForTopPerformers.invoke()).doReturn(flowOf(today))
        whenever(getSelectedRangeForDashboardStats.invoke()).doReturn(flowOf(today))

        whenever(
            updateAnalyticsData.invoke(
                selectedRange = today,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)

        sut.invoke()

        verify(updateAnalyticsData).invoke(
            selectedRange = today,
            forceCardUpdates = forceCardUpdates
        )
    }

    @Test
    fun `when one update fails then return false`() = runTest {
        val forceCardUpdates = listOf(AnalyticsCards.Products, AnalyticsCards.Revenue, AnalyticsCards.Session)

        whenever(getSelectedRangeForTopPerformers.invoke()).doReturn(flowOf(today))
        whenever(getSelectedRangeForDashboardStats.invoke()).doReturn(flowOf(yesterday))

        whenever(
            updateAnalyticsData.invoke(
                selectedRange = today,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)

        whenever(
            updateAnalyticsData.invoke(
                selectedRange = yesterday,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(false)

        val result = sut.invoke()

        assertFalse(result)
    }

    @Test
    fun `when all updates succeed then return true`() = runTest {
        val forceCardUpdates = listOf(AnalyticsCards.Products, AnalyticsCards.Revenue, AnalyticsCards.Session)

        whenever(getSelectedRangeForTopPerformers.invoke()).doReturn(flowOf(today))
        whenever(getSelectedRangeForDashboardStats.invoke()).doReturn(flowOf(yesterday))

        whenever(
            updateAnalyticsData.invoke(
                selectedRange = today,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)

        whenever(
            updateAnalyticsData.invoke(
                selectedRange = yesterday,
                forceCardUpdates = forceCardUpdates
            )
        ).doReturn(true)

        val result = sut.invoke()

        assertTrue(result)
    }
}
