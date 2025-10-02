package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.utils.json

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
}
