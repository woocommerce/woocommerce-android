package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

class AiAssistantOrderCardRendererTest {
    private val currencyFormatter: AiAssistantCurrencyFormatter = mock()
    private val context: Context = mock()
    private val selectedSite: SelectedSite = mock()
    private val crashLogger: CrashLogging = mock()
    private val dateUtils = DateUtils(Locale.US, crashLogger, selectedSite)

    @Test
    fun `when renderer is created, then class has direct unit test coverage`() {
        assertThat(AiAssistantOrderCardRenderer(currencyFormatter, dateUtils)).isNotNull
    }

    @Test
    fun `given assistant order card, when mapped, then host row model formats total with order currency`() {
        givenSiteTimezone()
        whenever(currencyFormatter.formatCurrency("12.34", "USD")).thenReturn("$12.34")

        val model = orderCard().toOrderSummaryRowModel(context, currencyFormatter, dateUtils)

        assertThat(model.number).isEqualTo("#1001")
        assertThat(model.date).isEqualTo("May 1")
        assertThat(model.customerName).isEqualTo("Jane Doe")
        assertThat(model.status).isEqualTo("processing")
        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
        assertThat(model.totalPrice).isEqualTo("$12.34")
        assertThat(model.isPosOrder).isFalse()
        verify(currencyFormatter).formatCurrency("12.34", "USD")
    }

    @Test
    fun `given assistant order card without currency, when mapped, then raw total is used`() {
        givenSiteTimezone()

        val model = orderCard(currency = "").toOrderSummaryRowModel(context, currencyFormatter, dateUtils)

        assertThat(model.totalPrice).isEqualTo("12.34")
    }

    @Test
    fun `given assistant order card with unparseable date, when mapped, then raw date is preserved`() {
        givenSiteTimezone()

        val model = orderCard(currency = "", date = "not-a-date")
            .toOrderSummaryRowModel(context, currencyFormatter, dateUtils)

        assertThat(model.date).isEqualTo("not-a-date")
    }

    @Test
    fun `given order date crosses selected site timezone day boundary, when mapped, then site local date is shown`() {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        givenSiteTimezone("2")

        val model = try {
            orderCard(
                currency = "",
                date = "${currentSiteYear()}-05-01T23:30:00Z",
            ).toOrderSummaryRowModel(context, currencyFormatter, dateUtils)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }

        assertThat(model.date).isEqualTo("May 2")
    }

    @Test
    fun `given assistant order card with blank customer name, when mapped, then guest fallback is used`() {
        givenSiteTimezone()
        whenever(context.getString(R.string.orderdetail_customer_name_default)).thenReturn("Guest")

        val model = orderCard(currency = "", customerName = "")
            .toOrderSummaryRowModel(context, currencyFormatter, dateUtils)

        assertThat(model.customerName).isEqualTo("Guest")
    }

    private fun givenSiteTimezone(timezone: String = SITE_TIMEZONE) {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { this.timezone = timezone })
    }

    private fun orderCard(
        status: String = "processing",
        currency: String = "USD",
        date: String = "${currentSiteYear()}-05-01T10:00:00Z",
        customerName: String = "Jane Doe",
    ) = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = status,
        total = "12.34",
        currency = currency,
        customerName = customerName,
        date = date,
    )

    private fun currentSiteYear(): Int = LocalDate.now(ZoneId.of(SITE_ZONE_ID)).year

    private companion object {
        const val SITE_TIMEZONE = "2"
        const val SITE_ZONE_ID = "Europe/Berlin"
    }
}
