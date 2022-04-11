package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.ResourceProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.util.*

const val SALE_DATES_PRICING_GROUP_KEY = "SALE"

const val TEXT_FROM = "FROM {START_DATE}"
const val TEXT_UNTIL = "UNTIL {END_DATE}"
const val TEXT_FROM_TO = "{START_DATE} - {END_DATE}"

class PriceUtilsTest {
    private val dateUtils = DateUtils(
        Locale.US,
        mock()
    )

    private val resourcesProviderMock = mock<ResourceProvider> {
        on { getString(R.string.product_sale_dates) }.thenReturn(SALE_DATES_PRICING_GROUP_KEY)
        on { getString(eq(R.string.product_sale_date_from), any()) }.thenReturn(TEXT_FROM)
        on { getString(eq(R.string.product_sale_date_to), any()) }.thenReturn(TEXT_UNTIL)
        on { getString(eq(R.string.product_sale_date_from_to), any(), any()) }
            .thenReturn(TEXT_FROM_TO)
    }

    @Test
    fun `Given both sale dates, when getPriceGroup, then the sale dates text is formatted correctly`() {
        val saleDatesText = getSaleDatesPricingGroupValue(
            saleStartDateGmt = dateUtils.getDateAtStartOfDay(2020, 11, 1),
            saleEndDateGmt = dateUtils.getDateAtStartOfDay(2020, 11, 31),
        )

        assertEquals(TEXT_FROM_TO, saleDatesText)
    }

    @Test
    fun `Given only the start sale date, when getPriceGroup, then the sale dates text is formatted correctly`() {
        val saleDatesText = getSaleDatesPricingGroupValue(
            saleStartDateGmt = dateUtils.getDateAtStartOfDay(2020, 11, 1),
        )

        assertEquals(TEXT_FROM, saleDatesText)
    }

    @Test
    fun `Given only the end sale date, when getPriceGroup, then the sale dates text is formatted correctly`() {
        val saleDatesText = getSaleDatesPricingGroupValue(
            saleEndDateGmt = dateUtils.getDateAtStartOfDay(2020, 11, 31),
        )

        assertEquals(TEXT_UNTIL, saleDatesText)
    }

    private fun getSaleDatesPricingGroupValue(
        saleStartDateGmt: Date? = null,
        saleEndDateGmt: Date? = null,
    ) = PriceUtils.getPriceGroup(
        SiteParameters(
            null,
            null,
            null,
            null,
            null,
            -2.0F,
        ),
        resourcesProviderMock,
        mock(),
        regularPrice = BigDecimal(100),
        salePrice = BigDecimal(75),
        isSaleScheduled = true,
        saleStartDateGmt = saleStartDateGmt,
        saleEndDateGmt = saleEndDateGmt,
    )[SALE_DATES_PRICING_GROUP_KEY]
}
