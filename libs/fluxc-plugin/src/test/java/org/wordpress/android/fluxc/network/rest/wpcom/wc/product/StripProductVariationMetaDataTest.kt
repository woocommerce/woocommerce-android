package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.StripProductVariationMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData

class StripProductVariationMetaDataTest {
    private val gson = Gson()
    private val sut = StripProductVariationMetaData(gson)

    @Test
    fun `when metadata contains not supported keys, then NOT supported keys are stripped`() {
        val result = sut.invoke(notOnlySupportedMetadata)
        val jsonResult = gson.fromJson(result, JsonArray::class.java)

        jsonResult.map { it as? JsonObject }.forEach { jsonObject ->
            assertThat(jsonObject).isNotNull
            assertThat(jsonObject?.get(WCMetaData.KEY)?.asString)
                .isIn(StripProductVariationMetaData.SUPPORTED_KEYS)
        }
    }

    @Test
    fun `when metadata contains a supported key with a NULL value, then strip the supported key`() {
        val supportedKey = StripProductVariationMetaData.SUPPORTED_KEYS.first()
        val value: String? = null

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        assertThat(result).isNull()
    }

    @Test
    fun `when metadata contains a supported key with a EMPTY value, then strip the supported key`() {
        val supportedKey = StripProductVariationMetaData.SUPPORTED_KEYS.first()
        val value = ""

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        assertThat(result).isNull()
    }

    @Test
    fun `when metadata contains a supported key with a NOT EMPTY value, then value is kept`() {
        val supportedKey = StripProductVariationMetaData.SUPPORTED_KEYS.first()
        val value = "valid"

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(metadata)

        val jsonResult = gson.fromJson(result, JsonArray::class.java)
        val resultItem = jsonResult[0] as JsonObject
        assertThat(result).isNotNull
        assertThat(resultItem[WCMetaData.KEY].asString).isEqualTo(supportedKey)
        assertThat(resultItem[WCMetaData.VALUE].asString).isEqualTo(value)
    }

    @Test
    fun `when metadata is null, then null is return`() {
        val result = sut.invoke(null)
        assertThat(result).isNull()
    }

    @Test
    fun `when metadata is empty, then null is return`() {
        val result = sut.invoke("")
        assertThat(result).isNull()
    }

    @Test
    fun `when metadata contains a supported key with an ARRAY value, then value is kept`() {
        val supportedKey = StripProductVariationMetaData.SUPPORTED_KEYS.first()
        val value = JsonArray().apply {
            add("Value 1")
            add("Value 2")
        }
        val item = JsonObject().apply {
            addProperty(WCMetaData.KEY, supportedKey)
            add(WCMetaData.VALUE, value)
        }
        val jsonArray = JsonArray().apply { add(item) }
        val metadata = gson.toJson(jsonArray)

        val result = sut.invoke(metadata)

        val jsonResult = gson.fromJson(result, JsonArray::class.java)
        val resultItem = jsonResult[0] as JsonObject
        assertThat(result).isNotNull
        assertThat(resultItem[WCMetaData.KEY].asString).isEqualTo(supportedKey)
        assertThat(resultItem[WCMetaData.VALUE].asJsonArray).isEqualTo(value)
    }

    private fun getOneItemMetadata(itemKey: String, itemValue: String?): String {
        val item = JsonObject().apply {
            addProperty(WCMetaData.KEY, itemKey)
            itemValue?.let {
                addProperty(WCMetaData.VALUE, it)
            } ?: add(WCMetaData.VALUE, null)
        }
        val jsonArray = JsonArray().apply {
            add(item)
        }

        return gson.toJson(jsonArray)
    }

    private val notOnlySupportedMetadata = """
        [
          {
            "id": 4464,
            "key": "_subscription_period",
            "value": "week"
          },
          {
            "id": 4465,
            "key": "_subscription_period_interval",
            "value": "1"
          },
          {
            "id": 4466,
            "key": "_subscription_length",
            "value": "0"
          },
          {
            "id": 4467,
            "key": "_subscription_trial_period",
            "value": "month"
          },
          {
            "id": 4674,
            "key": "_not_supported_metadata",
            "value": "product"
          },
          {
            "id": 4678,
            "key": "not_supported_metadata",
            "value": "no"
          },
          {
            "id": 4679,
            "key": "variation_group_of_quantity",
            "value": ""
          },
          {
            "id": 4680,
            "key": "variation_minimum_allowed_quantity",
            "value": ""
          },
          {
            "id": 4681,
            "key": "variation_maximum_allowed_quantity",
            "value": ""
          },
          {
            "id": 4682,
            "key": "variation_minmax_do_not_count",
            "value": "no"
          },
          {
            "id": 4683,
            "key": "variation_minmax_cart_exclude",
            "value": "no"
          },
          {
            "id": 4684,
            "key": "variation_minmax_category_group_of_exclude",
            "value": "no"
          },
          {
            "id": 4685,
            "key": "_subscription_sign_up_fee",
            "value": "0"
          },
          {
            "id": 4686,
            "key": "_subscription_price",
            "value": "5"
          },
          {
            "id": 4687,
            "key": "_subscription_trial_length",
            "value": "0"
          },
          {
            "id": 4688,
            "key": "_subscription_payment_sync_date",
            "value": "0"
          },
          {
            "id": 4726,
            "key": "_wcpay_product_hash",
            "value": "ece89db348e4ba7b4cfd7a0bd37dd178"
          },
          {
            "id": 4727,
            "key": "_wcpay_product_id_test",
            "value": "prod_NEJorUIqZTYOjW"
          },
          {
            "id": 8955,
            "key": "_wc_gla_mc_status",
            "value": "not_synced"
          }
        ]
    """.trimIndent()
}
