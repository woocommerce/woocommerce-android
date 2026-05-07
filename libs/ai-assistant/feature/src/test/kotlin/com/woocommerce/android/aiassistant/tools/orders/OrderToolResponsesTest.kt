package com.woocommerce.android.aiassistant.tools.orders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

@OptIn(ExperimentalCoroutinesApi::class)
class OrderToolResponsesTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun `given order line items, when detail response is built, then compact items are capped with markers`() =
        runTest {
            val order = order(
                lineItems = (1..12).joinToString(prefix = "[", postfix = "]") { index ->
                    """
                    {
                      "id":$index,
                      "name":"Item $index",
                      "quantity":2,
                      "sku":"SKU-$index",
                      "total":"20.00",
                      "product_id":$index,
                      "variation_id":0
                    }
                    """.trimIndent()
                }
            )

            val structured = json.encodeToJsonElement(order.toOrderDetailResponse(extraFields = emptySet())).jsonObject

            assertThat(structured.getValue("line_items_count").jsonPrimitive.int).isEqualTo(12)
            assertThat(structured.getValue("line_items_truncated").jsonPrimitive.boolean).isTrue
            val lineItems = structured.getValue("line_items").jsonArray
            assertThat(lineItems).hasSize(10)
            assertThat(lineItems.first().jsonObject.keys).containsExactly(
                "id", "name", "quantity", "sku", "total", "product_id", "variation_id"
            )
        }

    @Test
    fun `given billing and shipping extras, when detail response is built, then compact addresses are included`() =
        runTest {
            val response = order(
                billingPhone = "555-0100",
                billingCity = "Portland",
                billingCountry = "US",
                shippingCity = "Seattle",
                shippingCountry = "US",
            ).toOrderDetailResponse(extraFields = setOf("billing", "shipping"))

            val structured = json.encodeToJsonElement(response).jsonObject
            assertThat(structured.getValue("billing").jsonObject.getValue("phone").jsonPrimitive.content)
                .isEqualTo("555-0100")
            assertThat(structured.getValue("shipping").jsonObject.getValue("city").jsonPrimitive.content)
                .isEqualTo("Seattle")
        }

    @Test
    fun `given order adjustment extras, when detail response is built, then coupon fee and tax lines are projected`() =
        runTest {
            val response = order(
                couponLines = """[{"id":10,"code":"SAVE10","discount":"5.00","discount_tax":"0.50"}]""",
                feeLines = """[{"id":20,"name":"Rush","total":"7.00","total_tax":"0.70","tax_status":"taxable"}]""",
                taxLines = """
                    [
                      {
                        "id":30,
                        "rate_id":40,
                        "rate_code":"US-CA",
                        "label":"CA Tax",
                        "tax_total":"3.00",
                        "shipping_tax_total":"0.30"
                      }
                    ]
                """.trimIndent(),
            ).toOrderDetailResponse(extraFields = setOf("coupon_lines", "fee_lines", "tax_lines"))

            val structured = json.encodeToJsonElement(response).jsonObject
            val coupon = structured.getValue("coupon_lines").jsonArray.single().jsonObject
            assertThat(coupon.getValue("code").jsonPrimitive.content).isEqualTo("SAVE10")
            assertThat(coupon.getValue("discount").jsonPrimitive.content).isEqualTo("5.00")

            val fee = structured.getValue("fee_lines").jsonArray.single().jsonObject
            assertThat(fee.getValue("name").jsonPrimitive.content).isEqualTo("Rush")
            assertThat(fee.getValue("tax_status").jsonPrimitive.content).isEqualTo("taxable")

            val tax = structured.getValue("tax_lines").jsonArray.single().jsonObject
            assertThat(tax.getValue("rate_code").jsonPrimitive.content).isEqualTo("US-CA")
            assertThat(tax.getValue("tax_total").jsonPrimitive.content).isEqualTo("3.00")
        }

    private fun order(
        lineItems: String = "[]",
        billingPhone: String = "",
        billingCity: String = "",
        billingCountry: String = "",
        shippingCity: String = "",
        shippingCountry: String = "",
        couponLines: String = "",
        feeLines: String = "",
        taxLines: String = "",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 123L,
        number = "123",
        status = "processing",
        total = "45.00",
        currency = "USD",
        dateCreated = "2026-05-01T10:00:00Z",
        dateModified = "2026-05-02T10:00:00Z",
        datePaid = "2026-05-01T10:30:00Z",
        customerNote = "Leave at front desk",
        totalTax = "4.00",
        shippingTotal = "6.00",
        discountTotal = "2.00",
        customerId = 55L,
        billingFirstName = "Jane",
        billingLastName = "Doe",
        billingEmail = "jane@example.com",
        billingPhone = billingPhone,
        billingCity = billingCity,
        billingCountry = billingCountry,
        shippingCity = shippingCity,
        shippingCountry = shippingCountry,
        paymentMethodTitle = "Credit Card",
        lineItems = lineItems,
        couponLines = couponLines,
        feeLines = feeLines,
        taxLines = taxLines,
    )
}
