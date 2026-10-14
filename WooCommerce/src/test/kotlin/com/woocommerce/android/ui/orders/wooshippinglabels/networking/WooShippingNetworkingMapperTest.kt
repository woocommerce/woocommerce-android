package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingNetworkingMapperTest : BaseUnitTest() {
    private val sut = WooShippingNetworkingMapper(WooShippingRatesDatasourceMapper())
    private val defaultCustomsData = CustomsData(
        contentType = ContentType.MERCHANDISE,
        contentDescription = "",
        restrictionType = RestrictionType.NONE,
        restrictionDescription = "",
        isReturnToSender = false,
        itn = "",
        items = listOf(defaultCustomsItem)
    )

    @Test
    fun `when identifiers are requested, then map trimmed values and the no barcode code`() {
        val customsData = defaultCustomsData.copy(
            euProductIdentifiersRequirement = EuProductIdentifiersRequirement.REQUIRED,
            items = listOf(
                defaultCustomsItem.copy(
                    euProductIdentifiers = EuProductIdentifiers(" SKU ", " MPN ", " GTIN ")
                ),
                defaultCustomsItem.copy(
                    productID = 2L,
                    euProductIdentifiers = EuProductIdentifiers("SKU", "MPN", hasNoBarcode = true)
                )
            )
        )

        val items = sut.toCustomsItemDTOs(customsData).map {
            Triple(it.code, it.manufacturerProductId, it.standardizedProductId)
        }

        assertThat(items).containsExactly(
            Triple("SKU", "MPN", "GTIN"),
            Triple("SKU", "MPN", EuProductIdentifiers.NO_BARCODE_STANDARDIZED_PRODUCT_ID)
        )
    }

    @Test
    fun `when content is documents or requirement is not required, then omit identifiers`() {
        val item = defaultCustomsItem.copy(
            euProductIdentifiers = EuProductIdentifiers("SKU", "MPN", "GTIN")
        )
        val customsDataList = listOf(
            defaultCustomsData.copy(
                contentType = ContentType.DOCUMENTS,
                euProductIdentifiersRequirement = EuProductIdentifiersRequirement.REQUIRED,
                items = listOf(item)
            ),
            defaultCustomsData.copy(
                euProductIdentifiersRequirement = EuProductIdentifiersRequirement.NOT_REQUIRED,
                items = listOf(item)
            )
        )

        customsDataList.forEach { customsData ->
            val mappedItem = sut.toCustomsItemDTOs(customsData).single()

            assertThat(mappedItem.code).isNull()
            assertThat(mappedItem.manufacturerProductId).isNull()
            assertThat(mappedItem.standardizedProductId).isNull()
        }
    }

    private companion object {
        val defaultCustomsItem = CustomsItem(
            productID = 1L,
            description = "Item",
            quantity = 1f,
            value = BigDecimal.TEN,
            weight = 1f,
            hsTariffNumber = "123456",
            originCountry = "United States",
            originCountryCode = "US"
        )
    }
}
