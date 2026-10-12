package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.network.rest.GsonRequest

class WooPaymentsDepositsOverviewApiResponseTest {
    private val gson = GsonRequest.getDefaultGsonBuilder().create()

    @Test
    fun `given manual deposits with datetime string dates, when parsing the response, then the dates are kept as strings`() {
        // GIVEN
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, "wc/payments-deposits-overview-all.json")

        // WHEN
        val response = gson.fromJson(json, WooPaymentsDepositsOverviewApiResponse::class.java)

        // THEN
        val manualDeposit = response.deposit?.lastManualDeposits?.single()
        assertThat(manualDeposit?.currency).isEqualTo("usd")
        assertThat(manualDeposit?.date).isEqualTo("2026-09-28 12:05:11")
        assertThat(response.deposit?.lastPaid?.single()?.date).isEqualTo(1696550400000L)
    }
}
