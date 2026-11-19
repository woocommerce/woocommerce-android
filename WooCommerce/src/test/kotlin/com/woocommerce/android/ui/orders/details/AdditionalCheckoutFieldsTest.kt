package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.ui.orders.details.AdditionalCheckoutField.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue

class AdditionalCheckoutFieldsTest {
    @Test
    fun `given meta with each prefix, when toAdditionalCheckoutFields, then fields are grouped by prefix`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_billing/my-plugin/gov-id", value = "B-123"),
            WCMetaData(id = 2, key = "_wc_shipping/my-plugin/gov-id", value = "S-456"),
            WCMetaData(id = 3, key = "_wc_other/my-plugin/gift-message", value = "Happy birthday")
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.billing).containsExactly(AdditionalCheckoutField("Gov id", Value.Text("B-123")))
        assertThat(result.shipping).containsExactly(AdditionalCheckoutField("Gov id", Value.Text("S-456")))
        assertThat(result.other).containsExactly(AdditionalCheckoutField("Gift message", Value.Text("Happy birthday")))
    }

    @Test
    fun `given key with dashes and underscores, when toAdditionalCheckoutFields, then label is built from field id`() {
        // GIVEN
        val metadata = listOf(WCMetaData(id = 1, key = "_wc_other/ns/delivery_time-slot", value = "evening"))

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other.single().label).isEqualTo("Delivery time slot")
    }

    @Test
    fun `given values 1 and 0, when toAdditionalCheckoutFields, then they are checkbox values`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_other/ns/newsletter", value = "1"),
            WCMetaData(id = 2, key = "_wc_other/ns/terms", value = "0")
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other.map { it.value }).containsExactly(Value.Checkbox(true), Value.Checkbox(false))
    }

    @Test
    fun `given boolean value, when toAdditionalCheckoutFields, then it is a checkbox value`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_other/ns/newsletter", value = WCMetaDataValue(true))
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other.single().value).isEqualTo(Value.Checkbox(true))
    }

    @Test
    fun `given blank and JSON values, when toAdditionalCheckoutFields, then they are skipped`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_other/ns/empty", value = ""),
            WCMetaData(id = 2, key = "_wc_other/ns/blank", value = "   "),
            WCMetaData(id = 3, key = "_wc_other/ns/json", value = "{\"a\":\"b\"}")
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other).isEmpty()
    }

    @Test
    fun `given value with surrounding spaces, when toAdditionalCheckoutFields, then value is trimmed`() {
        // GIVEN
        val metadata = listOf(WCMetaData(id = 1, key = "_wc_other/ns/note", value = "  Hi there  "))

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other.single().value).isEqualTo(Value.Text("Hi there"))
    }

    @Test
    fun `given unrelated keys, when toAdditionalCheckoutFields, then they are ignored`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_foo/ns/field", value = "x"),
            WCMetaData(id = 2, key = "_wc_order_attribution_source_type", value = "utm"),
            WCMetaData(id = 3, key = "custom key", value = "custom value")
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.isEmpty).isTrue()
    }

    @Test
    fun `given several fields, when toAdditionalCheckoutFields, then server order is kept`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_other/ns/zeta", value = "z"),
            WCMetaData(id = 2, key = "_wc_other/ns/alpha", value = "a")
        )

        // WHEN
        val result = metadata.toAdditionalCheckoutFields()

        // THEN
        assertThat(result.other.map { it.label }).containsExactly("Zeta", "Alpha")
    }

    @Test
    fun `given empty metadata, when toAdditionalCheckoutFields, then result is empty`() {
        // WHEN
        val result = emptyList<WCMetaData>().toAdditionalCheckoutFields()

        // THEN
        assertThat(result.isEmpty).isTrue()
    }

    @Test
    fun `given additional checkout field meta, when isDisplayable, then it is false`() {
        // GIVEN
        val metadata = listOf(
            WCMetaData(id = 1, key = "_wc_billing/ns/field", value = "x"),
            WCMetaData(id = 2, key = "_wc_shipping/ns/field", value = "x"),
            WCMetaData(id = 3, key = "_wc_other/ns/field", value = "x")
        )

        // WHEN
        val result = metadata.map { it.isDisplayable }

        // THEN
        assertThat(result).containsOnly(false)
    }
}
