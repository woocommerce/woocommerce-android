package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class GetReportUrlTest : BaseUnitTest() {
    private val sampleAdminURL = "https://testing.com/wp-admin/"
    private val defaultSite = SiteModel().apply {
        adminUrl = sampleAdminURL
    }
    val selectedSite: SelectedSite = mock()

    val formatter = SimpleDateFormat("yyyy-MM-dd")

    private lateinit var sut: GetReportUrl

    @Before
    fun setup() {
        sut = GetReportUrl(selectedSite)
    }

    @Test
    fun `when the selected site returns null then the report url should be null`() {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val selectedRange = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
            referenceStartDate = formatter.parse("2024-02-02")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL = null
        val result = sut(selection = selectedRange, card = ReportCard.Revenue)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when today range is selected then return expected URL for revenue`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
            referenceStartDate = formatter.parse("2024-02-02")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue&period=today&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Revenue)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when today range is selected then return expected URL for orders`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
            referenceStartDate = formatter.parse("2024-02-02")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Forders&period=today&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Orders)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when today range is selected then return expected URL for products`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
            referenceStartDate = formatter.parse("2024-02-02")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Fproducts&period=today&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Products)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when quarter to date range is selected then return expected URL for revenue`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-01")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue&period=quarter&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Revenue)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when quarter to date range is selected then return expected URL for orders`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-01")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Forders&period=quarter&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Orders)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when quarter to date range is selected then return expected URL for products`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-01")!!,
            referenceEndDate = formatter.parse("2024-02-02")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Fproducts&period=quarter&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Products)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when last week range is selected then return expected URL for revenue`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.LAST_WEEK.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-27")!!,
            referenceEndDate = formatter.parse("2024-01-21")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue&period=last_week&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Revenue)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when last week range is selected then return expected URL for orders`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.LAST_WEEK.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-27")!!,
            referenceEndDate = formatter.parse("2024-01-21")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Forders&period=last_week&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Orders)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when last week range is selected then return expected URL for products`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val selectedRange = StatsTimeRangeSelection.SelectionType.LAST_WEEK.generateSelectionData(
            referenceStartDate = formatter.parse("2024-01-27")!!,
            referenceEndDate = formatter.parse("2024-01-21")!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Fproducts&period=last_week&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Products)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when custom range is selected then return expected URL for revenue`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val startDate = "2024-01-25"
        val endDate = "2024-01-20"
        val selectedRange = StatsTimeRangeSelection.SelectionType.CUSTOM.generateSelectionData(
            referenceStartDate = formatter.parse(startDate)!!,
            referenceEndDate = formatter.parse(endDate)!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue&period=custom&after=$startDate&before=$endDate&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Revenue)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when custom range is selected then return expected URL for orders`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val startDate = "2024-01-25"
        val endDate = "2024-01-20"
        val selectedRange = StatsTimeRangeSelection.SelectionType.CUSTOM.generateSelectionData(
            referenceStartDate = formatter.parse(startDate)!!,
            referenceEndDate = formatter.parse(endDate)!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Forders&period=custom&after=$startDate&before=$endDate&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Orders)
        assertEquals(result, expectedURL)
    }

    @Test
    fun `when custom range is selected then return expected URL for products`() {
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        val startDate = "2024-01-25"
        val endDate = "2024-01-20"
        val selectedRange = StatsTimeRangeSelection.SelectionType.CUSTOM.generateSelectionData(
            referenceStartDate = formatter.parse(startDate)!!,
            referenceEndDate = formatter.parse(endDate)!!,
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val expectedURL =
            "${sampleAdminURL}admin.php?page=wc-admin&path=%2Fanalytics%2Fproducts&period=custom&after=$startDate&before=$endDate&compare=previous_period"
        val result = sut(selection = selectedRange, card = ReportCard.Products)
        assertEquals(result, expectedURL)
    }
}
