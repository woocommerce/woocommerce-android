package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus.ServiceStatusNotice
import com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus.ServiceStatusNotice.Severity
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

    @Test
    fun `when service status has multiple notices, then map each with its severity`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(
                ServiceStatusNoticeDTO(
                    id = "usps",
                    type = "error",
                    message = "USPS: API Outage",
                    dismissible = false,
                    action = "https://www.easypoststatus.com"
                ),
                ServiceStatusNoticeDTO(id = "ups", type = "warning", message = "UPS: Degraded", dismissible = true),
                ServiceStatusNoticeDTO(id = "dhl", type = "info", message = "DHL: Maintenance")
            )
        )

        val notices = sut(response)

        assertThat(notices).containsExactly(
            ServiceStatusNotice(
                id = "usps",
                severity = Severity.ERROR,
                message = "USPS: API Outage",
                isDismissible = false,
                actionUrl = "https://www.easypoststatus.com"
            ),
            ServiceStatusNotice(
                id = "ups",
                severity = Severity.WARNING,
                message = "UPS: Degraded",
                isDismissible = true,
                actionUrl = null
            ),
            ServiceStatusNotice(
                id = "dhl",
                severity = Severity.INFO,
                message = "DHL: Maintenance",
                isDismissible = false,
                actionUrl = null
            )
        )
    }

    @Test
    fun `when service status notice type is uppercase, then map its severity`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(ServiceStatusNoticeDTO(id = "1", type = "ERROR", message = "Message"))
        )

        assertThat(sut(response).single().severity).isEqualTo(Severity.ERROR)
    }

    @Test
    fun `when service status notices contain a null entry, then skip it`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(null, ServiceStatusNoticeDTO(id = "1", type = "error", message = "Message"))
        )

        assertThat(sut(response).map { it.id }).containsExactly("1")
    }

    @Test
    fun `when service status notice has an unknown type, then map it as info`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(ServiceStatusNoticeDTO(id = "1", type = "critical", message = "Message"))
        )

        assertThat(sut(response).single().severity).isEqualTo(Severity.INFO)
    }

    @Test
    fun `when service status notice has no id or message, then drop it`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(
                ServiceStatusNoticeDTO(id = null, type = "error", message = "Message"),
                ServiceStatusNoticeDTO(id = " ", type = "error", message = "Message"),
                ServiceStatusNoticeDTO(id = "1", type = "error", message = ""),
                ServiceStatusNoticeDTO(id = "2", type = "error", message = "Valid")
            )
        )

        assertThat(sut(response).map { it.id }).containsExactly("2")
    }

    @Test
    fun `when service status notice action is not an http url, then drop the action`() {
        val response = ServiceStatusResponseDTO(
            notices = listOf(
                ServiceStatusNoticeDTO(id = "1", type = "error", message = "Message", action = "javascript:alert(1)"),
                ServiceStatusNoticeDTO(id = "2", type = "error", message = "Message", action = "not a url"),
                ServiceStatusNoticeDTO(id = "3", type = "error", message = "Message", action = "https:status.com"),
                ServiceStatusNoticeDTO(id = "4", type = "error", message = "Message", action = "HTTPS://status.com")
            )
        )

        assertThat(sut(response).map { it.actionUrl }).containsExactly(null, null, null, "HTTPS://status.com")
    }

    @Test
    fun `when service status has no notices, then return an empty list`() {
        assertThat(sut(ServiceStatusResponseDTO(notices = null))).isEmpty()
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
