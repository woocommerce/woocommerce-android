package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.utils.json

@Suppress("UnitTestNamingRule")
class WCRevenueStatsModelTest {
    @Test
    fun `should assign null value if number of sold items exceeds integer range`() {
        val sut = WCRevenueStatsModel(
            localSiteId = LocalId(1),
            interval = "",
            startDate = "",
            endDate = "",
            data = "",
            total = json { "num_items_sold" To "15032797007" }.toString(),
            rangeId = "",
        )

        val total = sut.parseTotal()

        assertThat(total?.itemsSold).isNull()
    }

    @Test
    fun `should assign null value if number of sold items is negative`() {
        val sut = WCRevenueStatsModel(
            localSiteId = LocalId(1),
            interval = "",
            startDate = "",
            endDate = "",
            data = "",
            total = json { "num_items_sold" To "-123" }.toString(),
            rangeId = "",
        )

        val total = sut.parseTotal()

        assertThat(total?.itemsSold).isNull()
    }

    @Test
    fun `should correctly parse value if number of sold items is within integer limits`() {
        val sut = WCRevenueStatsModel(
            localSiteId = LocalId(1),
            interval = "",
            startDate = "",
            endDate = "",
            data = "",
            total = json { "num_items_sold" To "123456" }.toString(),
            rangeId = "",
        )

        val total = sut.parseTotal()

        assertThat(total?.itemsSold).isEqualTo(123456)
    }

    @Test
    fun `should parse revenue sales types from total`() {
        val sut = WCRevenueStatsModel(
            localSiteId = LocalId(1),
            interval = "",
            startDate = "",
            endDate = "",
            data = "",
            total = json {
                "gross_sales" To "150.25"
                "net_revenue" To "120.15"
                "total_sales" To "170.35"
            }.toString(),
            rangeId = "",
        )

        val total = sut.parseTotal()

        assertThat(total?.grossSales).isEqualTo(150.25)
        assertThat(total?.netRevenue).isEqualTo(120.15)
        assertThat(total?.totalSales).isEqualTo(170.35)
    }

    @Test
    fun `should parse revenue sales types from intervals`() {
        val sut = WCRevenueStatsModel(
            localSiteId = LocalId(1),
            interval = "",
            startDate = "",
            endDate = "",
            data = """
                [
                    {
                        "interval": "2026-04-27",
                        "subtotals": {
                            "gross_sales": 45.25,
                            "net_revenue": 30.15,
                            "total_sales": 50.35
                        }
                    }
                ]
            """.trimIndent(),
            total = "",
            rangeId = "",
        )

        val interval = sut.getIntervalList().first()

        assertThat(interval.subtotals?.grossSales).isEqualTo(45.25)
        assertThat(interval.subtotals?.netRevenue).isEqualTo(30.15)
        assertThat(interval.subtotals?.totalSales).isEqualTo(50.35)
    }
}
