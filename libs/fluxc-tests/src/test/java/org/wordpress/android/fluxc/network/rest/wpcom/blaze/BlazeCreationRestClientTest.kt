package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeBillingSummary
import org.wordpress.android.fluxc.network.rest.GsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.test
import java.util.Date

class BlazeCreationRestClientTest {
    private val wpComNetwork: WPComNetwork = mock()
    private val site = SiteModel().apply { siteId = SITE_ID }

    private lateinit var restClient: BlazeCreationRestClient

    @Before
    fun setUp() {
        restClient = BlazeCreationRestClient(wpComNetwork)
    }

    @Test
    fun `given a response with debt, when billing summary is requested, then debt and payment links are parsed`() =
        test {
            givenBillingSummaryResponse(json = readJson(BILLING_SUMMARY_JSON))

            val result = restClient.fetchBillingSummary(site)

            assertThat(result.isError).isFalse()
            assertThat(result.data).isEqualTo(
                BlazeBillingSummary(
                    debt = 25.05,
                    paymentLinks = listOf(
                        BlazeBillingSummary.PaymentLink(
                            date = Date(PAYMENT_LINK_DATE_MILLIS),
                            amount = 25.05,
                            url = "https://example.com/pay/826745"
                        )
                    )
                )
            )
        }

    @Test
    fun `given invalid values, when billing summary is requested, then they fall back to defaults`() = test {
        givenBillingSummaryResponse(
            json = """{"debt": "", "payment_links": [{"date": "not a date", "url": "https://example.com/pay/1"}]}"""
        )

        val result = restClient.fetchBillingSummary(site)

        assertThat(result.data).isEqualTo(
            BlazeBillingSummary(
                debt = 0.0,
                paymentLinks = listOf(
                    BlazeBillingSummary.PaymentLink(date = null, amount = 0.0, url = "https://example.com/pay/1")
                )
            )
        )
    }

    private suspend fun givenBillingSummaryResponse(json: String) {
        val data = GsonRequest.getDefaultGsonBuilder().create()
            .fromJson(json, BlazeBillingSummaryResponse::class.java)
        val response = Response.Success(data, emptyList())

        whenever(
            wpComNetwork.executeGetGsonRequest(
                url = any(),
                clazz = eq(BlazeBillingSummaryResponse::class.java),
                params = any(),
                enableCaching = eq(false),
                cacheTimeToLive = any(),
                forced = eq(false)
            )
        ).thenReturn(response)
    }

    private fun readJson(fileName: String) = UnitTestUtils.getStringFromResourceFile(javaClass, fileName)

    private companion object {
        const val SITE_ID = 12L
        const val BILLING_SUMMARY_JSON = "wp/blaze/blaze-billing-summary.json"

        // 2025-06-19T00:12:09.000Z
        const val PAYMENT_LINK_DATE_MILLIS = 1_750_291_929_000L
    }
}
