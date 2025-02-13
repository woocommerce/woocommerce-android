package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.fluxc.model.StripProductMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData.AddOnsMetadataKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys

class StripProductMetaDataTest {
    private val gson = Gson()
    private val sut = StripProductMetaData()

    private val notOnlySupportedMetadata = gson.fromJson(metaDataJson, Array<WCMetaData>::class.java).toList()

    @Test
    fun `when filtering metadata, then remove unsupported keys and values`() {
        val result = sut.invoke(notOnlySupportedMetadata)

        result.forEach { metaData ->
            Assertions.assertThat(metaData).isNotNull
            Assertions.assertThat(
                metaData.key in StripProductMetaData.SUPPORTED_KEYS ||
                    !metaData.isJson
            ).isTrue()
        }
    }

    @Test
    fun `when metadata contains a supported key with a NOT EMPTY value, then value is kept`() {
        val supportedKey = StripProductMetaData.SUPPORTED_KEYS.first()
        val value = "valid"

        val metadata = getOneItemMetadata(supportedKey, value)
        val result = sut.invoke(listOf(metadata))

        val resultItem = result.first()
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(resultItem.key).isEqualTo(supportedKey)
        Assertions.assertThat(resultItem.valueAsString).isEqualTo(value)
    }

    @Test
    fun `assert no regression in subscription metadata`() {
        Assertions.assertThat(StripProductMetaData.SUPPORTED_KEYS)
            .containsAll(SubscriptionMetadataKeys.ALL_KEYS)
    }

    @Test
    fun `assert no regression in add-ons metadata`() {
        Assertions.assertThat(StripProductMetaData.SUPPORTED_KEYS)
            .contains(AddOnsMetadataKeys.ADDONS_METADATA_KEY)
    }

    private fun getOneItemMetadata(itemKey: String, itemValue: String?) = WCMetaData(
        id = 0,
        key = itemKey,
        value = WCMetaDataValue.fromJsonElement(JsonPrimitive(itemValue))
    )

    companion object {
        private val metaDataJson = """
        [
          {
            "id": 11749,
            "key": "_wpcom_is_markdown",
            "value": "1"
          },
          {
            "id": 11750,
            "key": "_last_editor_used_jetpack",
            "value": "classic-editor"
          },
          {
            "id": 11754,
            "key": "_product_addons",
            "value": [{
                    "id": 11773,
                    "key": "_wc_gla_sync_status",
                    "value": "synced"
                  },
                  {
                    "id": 11774,
                    "key": "group_of_quantity",
                    "value": ""
                  }]
          },
          {
            "id": 11755,
            "key": "_product_addons_exclude_global",
            "value": "1"
          },
          {
            "id": 11772,
            "key": "_wc_gla_visibility",
            "value": "sync-and-show"
          },
          {
            "id": 11773,
            "key": "_wc_gla_sync_status",
            "value": "synced"
          },
          {
            "id": 11774,
            "key": "group_of_quantity",
            "value": ""
          },
          {
            "id": 11775,
            "key": "minimum_allowed_quantity",
            "value": ""
          },
          {
            "id": 11776,
            "key": "maximum_allowed_quantity",
            "value": ""
          },
          {
            "id": 11777,
            "key": "minmax_do_not_count",
            "value": "no"
          },
          {
            "id": 11778,
            "key": "minmax_cart_exclude",
            "value": "no"
          },
          {
            "id": 11779,
            "key": "minmax_category_group_of_exclude",
            "value": "no"
          },
          {
            "id": 11780,
            "key": "_wcsatt_disabled",
            "value": "yes"
          },
          {
            "id": 11781,
            "key": "_subscription_one_time_shipping",
            "value": "no"
          },
          {
            "id": 11782,
            "key": "_wcsatt_force_subscription",
            "value": "no"
          },
          {
            "id": 11785,
            "key": "_subscription_downloads_ids",
            "value": ""
          },
          {
            "id": 11786,
            "key": "minimum_allowed_quantity",
            "value": "40"
          },
          {
            "id": 11787,
            "key": "_wc_pre_orders_fee",
            "value": ""
          },
          {
            "id": 11788,
            "key": "_wpas_done_all",
            "value": "1"
          },
          {
            "id": 11909,
            "key": "_wc_gla_mc_status",
            "value": "not_synced"
          },
          {
            "id": 12406,
            "key": "_wc_gla_synced_at",
            "value": "1686193267"
          },
          {
            "id": 12407,
            "key": "_wc_gla_google_ids",
            "value": {
              "US": "online:en:US:gla_418"
            }
          },
          {
            "key": "_satt_data",
            "value": {
              "subscription_schemes": []
            }
          }
        ]
    """.trimIndent()
    }
}
