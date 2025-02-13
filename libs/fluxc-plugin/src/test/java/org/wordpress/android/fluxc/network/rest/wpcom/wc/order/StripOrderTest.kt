package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.CHARGE_ID_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.RECEIPT_URL_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.SHIPPING_PHONE_KEY
import org.wordpress.android.fluxc.utils.json

internal class StripOrderTest {
    lateinit var sut: StripOrder

    @Before
    fun setUp() {
        sut = StripOrder(Gson())
    }

    @Test
    fun `should ignore additional members in line item`() {
        // given
        val lineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To 1234
                    "name" To "iPhone"
                    redundantMemberKey To "not needed parameter"
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(lineItems = lineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.lineItems).contains(redundantMemberKey)
        assertThat(strippedOrder.lineItems).doesNotContain(redundantMemberKey)
    }

    @Test
    fun `should ignore internal-only remote line item attributes if applied to metadata key`() {
        // given
        val internalOnlyAttributeMemberKey = "_internal_attribute_key"
        val lineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To 1234
                    "name" To "iPhone"
                    "meta_data" To listOf(
                        json {
                            "key" To internalOnlyAttributeMemberKey
                            "value" To "sample value"
                        }
                    )
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(lineItems = lineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.lineItems).contains(internalOnlyAttributeMemberKey)
        assertThat(strippedOrder.lineItems).doesNotContain(internalOnlyAttributeMemberKey)
    }

    @Test
    fun `should ignore additional members in shipping item`() {
        // given
        val shippingLineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To "1234"
                    "method_id" To "3"
                    redundantMemberKey To "not needed parameter"
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(shippingLines = shippingLineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.shippingLines).contains(redundantMemberKey)
        assertThat(strippedOrder.shippingLines).doesNotContain(redundantMemberKey)
    }

    @Test
    fun `should ignore additional members in fee item`() {
        // given
        val feeLineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To "1234"
                    "totalTax" To "123$"
                    redundantMemberKey To "not needed parameter"
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(feeLines = feeLineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.feeLines).contains(redundantMemberKey)
        assertThat(strippedOrder.feeLines).doesNotContain(redundantMemberKey)
    }

    @Test
    fun `should ignore additional members in tax item`() {
        // given
        val taxLineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To "1234"
                    "rate_code" To "CODE"
                    redundantMemberKey To "not needed parameter"
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(taxLines = taxLineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.taxLines).contains(redundantMemberKey)
        assertThat(strippedOrder.taxLines).doesNotContain(redundantMemberKey)
    }

    @Test
    fun `should drop any not needed meta data`() {
        // given
        val metaDataFromRemote = listOf(
            WCMetaData(1, redundantMemberKey, "sample value"),
            WCMetaData(2, CHARGE_ID_KEY, "sample value"),
            WCMetaData(3, SHIPPING_PHONE_KEY, "sample value")
        )
        val fatModel = emptyOrder.copy(metaData = metaDataFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.metaData.map { it.key }).contains(
            redundantMemberKey,
            CHARGE_ID_KEY,
            SHIPPING_PHONE_KEY
        )
        assertThat(strippedOrder.metaData.map { it.key }).doesNotContain(redundantMemberKey)
            .contains(
                CHARGE_ID_KEY,
                SHIPPING_PHONE_KEY
            )
    }

    @Test
    fun `should not crash when item attribute key is empty`() {
        // given
        val emptyAttributeValue = "sample value"
        val lineItemsFromRemote = JsonArray().apply {
            add(
                json {
                    "id" To 1234
                    "name" To "iPhone"
                    "meta_data" To listOf(
                        json {
                            "key" To ""
                            "value" To emptyAttributeValue
                        }
                    )
                }
            )
        }.toString()
        val fatModel = emptyOrder.copy(lineItems = lineItemsFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.lineItems).contains(emptyAttributeValue)
        assertThat(strippedOrder.lineItems).contains(emptyAttributeValue)
    }

    @Test
    fun `should filter meta data that contains "receipt_url" as key`() {
        // given
        val metaDataFromRemote = listOf(
            WCMetaData(1, redundantMemberKey, "sample value"),
            WCMetaData(2, CHARGE_ID_KEY, "sample value"),
            WCMetaData(3, SHIPPING_PHONE_KEY, "sample value"),
            WCMetaData(4, RECEIPT_URL_KEY, "sample value")
        )

        val fatModel = emptyOrder.copy(metaData = metaDataFromRemote)

        // when
        val strippedOrder = sut.invoke(fatModel)

        // then
        assertThat(fatModel.metaData.map { it.key }).contains(
            redundantMemberKey,
            CHARGE_ID_KEY,
            SHIPPING_PHONE_KEY,
            RECEIPT_URL_KEY
        )
        assertThat(strippedOrder.metaData.map { it.key })
            .contains(
                CHARGE_ID_KEY,
                SHIPPING_PHONE_KEY,
                RECEIPT_URL_KEY
            )
    }

    companion object {
        const val redundantMemberKey = "not_needed_parameter"

        val emptyOrder = OrderEntity(
            localSiteId = LocalOrRemoteId.LocalId(0),
            orderId = 0
        )
    }
}
